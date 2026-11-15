package org.dpdns.syscrashed.phiclient.gui.music;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;
import org.lwjgl.Sys;

/**
 * The three import routes of the music subsystem (package
 * {@code org.dpdns.syscrashed.phiclient.gui.music}).
 *
 * <p>All routes copy the picked files into the music folder (the library never references external
 * paths) and skip unsupported or broken files. When the target name already exists in the music
 * folder, the file is never silently duplicated or overwritten: the collision is published through
 * {@link #pollNextConflict()} and the client tick presents the user the overwrite/cancel decision
 * ({@link #resolveConflict(ImportConflict, boolean)}). The system dialogs are reached through
 * reflection and the window drag-and-drop through a reflective {@code java.awt.dnd.DropTarget} on the
 * LWJGL display canvas, so the client does not need AWT on the class path and nothing breaks on
 * platforms without it: every route degrades to {@code false} with a log entry.</p>
 *
 * <p>The file dialog blocks the caller (it is modal); the drop events arrive on the AWT event thread,
 * so the import result, the rescan request and the collisions are published through
 * {@link #pollLastResult()}, {@link #consumePendingImport()} and {@link #pollNextConflict()} for the
 * client tick to consume.</p>
 */
public final class PhiMusicImporter {

    /**
     * The outcome of one import run.
     */
    public static final class ImportResult {

        private final int imported;
        private final int skipped;
        private final int failed;

        private ImportResult(int imported, int skipped, int failed) {
            this.imported = imported;
            this.skipped = skipped;
            this.failed = failed;
        }

        /**
         * @return the number of files that were copied into the library.
         */
        public int getImported() {
            return this.imported;
        }

        /**
         * @return the number of files that were skipped (already present or unsupported format).
         */
        public int getSkipped() {
            return this.skipped;
        }

        /**
         * @return the number of files that could not be copied.
         */
        public int getFailed() {
            return this.failed;
        }
    }

    /**
     * An import collision: the target name already exists in the music folder and the user has to
     * decide between overwriting the existing file or cancelling the import.
     */
    public static final class ImportConflict {

        private final File source;
        private final String targetName;

        private ImportConflict(File source, String targetName) {
            this.source = source;
            this.targetName = targetName;
        }

        /**
         * @return the picked file waiting to be imported.
         */
        public File getSource() {
            return this.source;
        }

        /**
         * @return the colliding file name inside the music folder.
         */
        public String getTargetName() {
            return this.targetName;
        }
    }

    /** The music folder the files are copied into. */
    private final File musicFolder;

    /** The last import result, waiting to be picked up (volatile: written on the AWT thread). */
    private volatile ImportResult lastResult;

    /** Import collisions waiting for the user's decision (written on the AWT thread, read on the client thread). */
    private final ConcurrentLinkedQueue<ImportConflict> conflicts = new ConcurrentLinkedQueue<ImportConflict>();

    /** True when a drop import succeeded and the library should rescan immediately. */
    private volatile boolean pendingRescan;

    /** True while the drop target accepts files (toggled by the hub open/close). */
    private volatile boolean dropEnabled;

    /** The installed drop target (kept alive to prevent garbage collection). */
    private Object dropTarget;

    /**
     * Creates the importer for the given music folder.
     *
     * @param musicFolder the folder the files are copied into.
     */
    public PhiMusicImporter(File musicFolder) {
        this.musicFolder = musicFolder;
    }

    /**
     * Opens the system file dialog (multi-select) and imports the picked files. Blocks until the
     * dialog closes. Must run on the client thread.
     *
     * @return the import result, or {@code null} when the dialog was cancelled or unavailable.
     */
    public ImportResult importViaDialog() {
        try {
            Class<?> dialogClass = Class.forName("java.awt.FileDialog");
            Class<?> frameClass = Class.forName("java.awt.Frame");
            Constructor<?> constructor = dialogClass.getConstructor(frameClass, String.class);
            Object dialog = constructor.newInstance(null, "Import music");
            dialogClass.getMethod("setMultipleMode", boolean.class).invoke(dialog, true);
            dialogClass.getMethod("setVisible", boolean.class).invoke(dialog, true);

            List<File> picked = new ArrayList<File>();
            Object files = dialogClass.getMethod("getFiles").invoke(dialog);
            if (files instanceof File[]) {
                for (File file : (File[]) files) {
                    if (file != null) {
                        picked.add(file);
                    }
                }
            }
            if (picked.isEmpty()) {
                // Older JDKs only expose the single file.
                Object single = dialogClass.getMethod("getFile").invoke(dialog);
                if (single instanceof File) {
                    picked.add((File) single);
                }
            }
            if (picked.isEmpty()) {
                return null; // cancelled
            }
            ImportResult result = copyToLibrary(picked);
            this.lastResult = result;
            return result;
        } catch (Throwable t) {
            PhiLogger.warn("PhiMusicImporter - the file dialog is not available on this platform");
            return null;
        }
    }

    /**
     * Opens the music folder in the system file manager.
     *
     * @return true when the folder could be opened.
     */
    public boolean openMusicFolder() {
        if (!this.musicFolder.exists()) {
            this.musicFolder.mkdirs();
        }
        try {
            Class<?> desktopClass = Class.forName("java.awt.Desktop");
            Object desktop = desktopClass.getMethod("getDesktop").invoke(null);
            desktopClass.getMethod("open", File.class).invoke(desktop, this.musicFolder);
            return true;
        } catch (Throwable t) {
            try {
                Sys.openURL(this.musicFolder.toURI().toString());
                return true;
            } catch (Throwable t2) {
                PhiLogger.warn("PhiMusicImporter - cannot open the music folder");
                return false;
            }
        }
    }

    /**
     * Installs the drag-and-drop listener on the LWJGL display canvas. Idempotent.
     *
     * @return true when drag and drop is available (false on platforms without AWT).
     */
    public boolean installDropTarget() {
        if (this.dropTarget != null) {
            return true;
        }
        try {
            Class<?> displayClass = Class.forName("org.lwjgl.opengl.Display");
            Object canvas = displayClass.getMethod("getParent").invoke(null);
            Class<?> componentClass = Class.forName("java.awt.Component");
            if (canvas == null || !componentClass.isInstance(canvas)) {
                return false;
            }

            final Class<?> listenerClass = Class.forName("java.awt.dnd.DropTargetListener");
            final Class<?> dropTargetClass = Class.forName("java.awt.dnd.DropTarget");
            final Class<?> dropEventClass = Class.forName("java.awt.dnd.DropTargetDropEvent");
            final Class<?> dragEventClass = Class.forName("java.awt.dnd.DropTargetDragEvent");
            final Class<?> transferableClass = Class.forName("java.awt.datatransfer.Transferable");
            final Class<?> flavorClass = Class.forName("java.awt.datatransfer.DataFlavor");
            final Object fileListFlavor = flavorClass.getField("javaFileListFlavor").get(null);
            final int copyAction = ((Integer) Class.forName("java.awt.dnd.DnDConstants")
                    .getField("ACTION_COPY").get(null)).intValue();

            InvocationHandler handler = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    String name = method.getName();
                    try {
                        if (name.equals("drop")) {
                            Object event = args[0];
                            if (!PhiMusicImporter.this.dropEnabled) {
                                dropEventClass.getMethod("rejectDrop").invoke(event);
                                return null;
                            }
                            Object transferable = dropEventClass.getMethod("getTransferable").invoke(event);
                            Object data = transferableClass.getMethod("getTransferData", flavorClass)
                                    .invoke(transferable, fileListFlavor);
                            List<File> files = new ArrayList<File>();
                            if (data instanceof List) {
                                for (Object item : (List<?>) data) {
                                    if (item instanceof File) {
                                        files.add((File) item);
                                    }
                                }
                            }
                            if (files.isEmpty()) {
                                dropEventClass.getMethod("rejectDrop").invoke(event);
                            } else {
                                dropEventClass.getMethod("acceptDrop", int.class).invoke(event, copyAction);
                                ImportResult result = copyToLibrary(files);
                                PhiMusicImporter.this.lastResult = result;
                                if (result.getImported() > 0) {
                                    PhiMusicImporter.this.pendingRescan = true;
                                }
                                dropEventClass.getMethod("dropComplete", boolean.class).invoke(event, true);
                            }
                        } else if (name.equals("dragEnter") || name.equals("dragOver")) {
                            if (PhiMusicImporter.this.dropEnabled) {
                                dragEventClass.getMethod("acceptDrag", int.class).invoke(args[0], copyAction);
                            } else {
                                dragEventClass.getMethod("rejectDrag").invoke(args[0]);
                            }
                        }
                        // dragExit / dropActionChanged need no action
                    } catch (Throwable ignored) {
                        // drop handling is best effort
                    }
                    return null;
                }
            };

            Object listener = Proxy.newProxyInstance(
                    Thread.currentThread().getContextClassLoader(),
                    new Class[] { listenerClass },
                    handler);
            Constructor<?> constructor = dropTargetClass.getConstructor(componentClass, listenerClass);
            this.dropTarget = constructor.newInstance(canvas, listener);
            return true;
        } catch (Throwable t) {
            PhiLogger.warn("PhiMusicImporter - drag and drop is not available on this platform");
            return false;
        }
    }

    /**
     * Enables or disables the drop import (called by the hub when it opens and closes, so files are
     * only imported while the hub is on screen).
     *
     * @param enabled true to accept files.
     */
    public void setDropEnabled(boolean enabled) {
        this.dropEnabled = enabled;
    }

    /**
     * @return the last import result and clears it (null when nothing is pending).
     */
    public ImportResult pollLastResult() {
        ImportResult result = this.lastResult;
        this.lastResult = null;
        return result;
    }

    /**
     * Consumes the rescan request raised by a drop import.
     *
     * @return true when a rescan was requested since the last call.
     */
    public boolean consumePendingImport() {
        boolean pending = this.pendingRescan;
        this.pendingRescan = false;
        return pending;
    }

    /**
     * Copies the picked files into the music folder.
     *
     * @param sources the picked files.
     * @return the import statistics.
     */
    private ImportResult copyToLibrary(List<File> sources) {
        int imported = 0;
        int skipped = 0;
        int failed = 0;

        for (File source : sources) {
            if (source == null || !source.isFile()) {
                failed++;
                continue;
            }
            if (!PhiAudioDecoder.isSupportedFile(source)) {
                skipped++;
                continue;
            }
            File target = new File(this.musicFolder, source.getName());
            if (target.getAbsolutePath().equals(source.getAbsolutePath())) {
                skipped++; // already inside the library
                continue;
            }
            if (target.exists()) {
                // A track with this name already exists - never silently duplicate or overwrite it;
                // the user decides through the published conflict.
                this.conflicts.add(new ImportConflict(source, source.getName()));
                continue;
            }
            try {
                Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                imported++;
            } catch (IOException e) {
                PhiLogger.warn("PhiMusicImporter - cannot import " + source.getName());
                failed++;
            }
        }

        return new ImportResult(imported, skipped, failed);
    }

    /**
     * @return true when import collisions are waiting for the user's decision.
     */
    public boolean hasPendingConflicts() {
        return !this.conflicts.isEmpty();
    }

    /**
     * @return the next import collision to decide, or {@code null} when none is pending.
     */
    public ImportConflict pollNextConflict() {
        return this.conflicts.poll();
    }

    /**
     * Applies the user's decision for an import collision. Must be called on the client thread.
     *
     * @param conflict  the collision (from {@link #pollNextConflict()}).
     * @param overwrite true to replace the existing file, false to cancel the import.
     * @return true when the decision was applied (overwritten or skipped), false on copy failure.
     */
    public boolean resolveConflict(ImportConflict conflict, boolean overwrite) {
        if (conflict == null) {
            return false;
        }
        if (!overwrite) {
            return true; // cancelled by the user
        }
        try {
            File target = new File(this.musicFolder, conflict.targetName);
            Files.copy(conflict.source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            this.pendingRescan = true;
            return true;
        } catch (IOException e) {
            PhiLogger.warn("PhiMusicImporter - cannot import " + conflict.targetName);
            return false;
        }
    }
}
