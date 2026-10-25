package org.dpdns.syscrashed.phiclient.gui.render;

import java.lang.reflect.Field;
import java.util.List;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;

import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;

/**
 * Reads the internal shader list of a {@link ShaderGroup}
 * (package {@code org.dpdns.syscrashed.phiclient.gui.render}).
 *
 * <p>MC 1.8.9's {@link ShaderGroup} has no public getter. Exposing the private {@code listShaders}
 * field through a Mixin would depend on a <b>field</b> mapping in the Mixin refmap (this project only
 * generates method / field mappings for MixinMinecraft, so a MixinShaderGroup mapping for
 * {@code listShaders} would be missing) and would fail to resolve in the obfuscated environment.</p>
 *
 * <p>Instead we reflect by <b>type</b>: iterate the {@code List}-typed fields of {@link ShaderGroup} and
 * return the first one whose elements are {@link Shader}. This depends on neither the field name nor
 * obfuscation / remapping, so it works in both dev (SRG) and release (notch) builds.</p>
 *
 * <p>Provided methods:</p>
 * <ul>
 *   <li>{@link #getShaders(ShaderGroup)} - returns the shader list, or {@code null} if it cannot be located.</li>
 * </ul>
 */
final class ShaderGroupAccess {

    private ShaderGroupAccess() {
    }

    /**
     * Locates and returns the shader list inside a {@link ShaderGroup}.
     *
     * @param group the shader group to inspect; may be {@code null} (returns {@code null}).
     * @return the shader list, or {@code null} if none could be found (a warning is logged then).
     */
    @SuppressWarnings("unchecked")
    static List<Shader> getShaders(ShaderGroup group) {
        if (group == null) {
            return null;
        }

        for (Field field : ShaderGroup.class.getDeclaredFields()) {
            if (!List.class.isAssignableFrom(field.getType())) {
                continue;
            }

            Object value;
            try {
                field.setAccessible(true);
                value = field.get(group);
            } catch (Throwable t) {
                continue;
            }

            if (!(value instanceof List)) {
                continue;
            }

            List<?> list = (List<?>) value;
            for (Object item : list) {
                if (item instanceof Shader) {
                    return (List<Shader>) list;
                }
            }
        }

        PhiLogger.warn("Could not locate the shader list inside ShaderGroup; blur uniforms are skipped.");
        return null;
    }
}
