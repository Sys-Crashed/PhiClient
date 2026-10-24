package org.dpdns.syscrashed.phiclient.gui.render;

import java.lang.reflect.Field;
import java.util.List;

import org.dpdns.syscrashed.phiclient.logger.PhiLogger;

import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;

/**
 * 取 {@link ShaderGroup} 内部的 shader 列表。
 *
 * <p>1.8.9 的 ShaderGroup 没有公开的 getter，原 SoarClient 是用 mixin 把私有字段
 * {@code listShaders} 暴露出来的。但那条路依赖 mixin refmap 里的<b>字段</b>映射
 * （本项目的 Mixin AP 只为 MixinMinecraft 生成了方法 / 字段映射，没有为
 * MixinShaderGroup 生成 {@code listShaders} 的映射），在混淆环境下会找不到字段。
 *
 * <p>所以这里改成按<b>类型</b>反射：遍历 ShaderGroup 的 List 型字段，取出其中第一个
 * 元素是 {@link Shader} 的那个。这样既不依赖字段名，也就不受混淆 / 重映射影响，
 * dev（SRG）与真机（notch）通用。
 */
final class ShaderGroupAccess {

    private ShaderGroupAccess() {
    }

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
