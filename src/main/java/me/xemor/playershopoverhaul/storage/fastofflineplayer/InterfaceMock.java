package me.xemor.playershopoverhaul.storage.fastofflineplayer;

import java.lang.reflect.*;

public class InterfaceMock {

    @SuppressWarnings("unchecked")
    public static <T> T mockInterface(Class<T> iface, MethodOverride... overrides) {
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{iface},
                (proxy, method, args) -> {
                    for (MethodOverride override : overrides) {
                        if (override.overrides(method)) {
                            return override.invoke();
                        }
                    }
                    throw new IllegalArgumentException("This method is mocked out in FastOfflinePlayer, don't attempt to use the placeholder!");
                });
    }

    public interface MethodOverride {
        boolean overrides(Method method);
        Object invoke();
    }
}
