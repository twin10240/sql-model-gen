package org.sqlmodel;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class JdbcProxyFixtures {
    final List<String> preparedSql = new ArrayList<String>();
    final List<Map<Integer, String>> parameters = new ArrayList<Map<Integer, String>>();
    int executeQueryCalls;
    int setMaxRowsCalls;
    int resultSetNextCalls;
    ResultSetMetaData directMetadata;
    ResultSet fallbackResultSet;
    ResultSet commentResultSet;

    Connection connection() {
        return proxy(Connection.class, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("prepareStatement".equals(method.getName())) {
                    preparedSql.add((String) args[0]);
                    Map<Integer, String> values = new HashMap<Integer, String>();
                    parameters.add(values);
                    return statement(!((String) args[0]).contains("ALL_COL_COMMENTS"), values);
                }
                return defaultValue(method.getReturnType());
            }
        });
    }

    private PreparedStatement statement(final boolean first, final Map<Integer, String> values) {
        return proxy(PreparedStatement.class, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("getMetaData".equals(name)) return directMetadata;
                if ("setString".equals(name)) { values.put((Integer) args[0], (String) args[1]); return null; }
                if ("setMaxRows".equals(name)) { setMaxRowsCalls++; return null; }
                if ("executeQuery".equals(name)) {
                    executeQueryCalls++;
                    return first ? fallbackResultSet : commentResultSet;
                }
                return defaultValue(method.getReturnType());
            }
        });
    }

    static ResultSetMetaData metadata(final String[] labels, final int[] types,
                                      final int[] precision, final int[] scale, final String[] typeNames) {
        return proxy(ResultSetMetaData.class, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("getColumnCount".equals(method.getName())) return labels.length;
                int index = args == null ? 0 : ((Integer) args[0]) - 1;
                if ("getColumnLabel".equals(method.getName())) return labels[index];
                if ("getColumnType".equals(method.getName())) return types[index];
                if ("getPrecision".equals(method.getName())) return precision[index];
                if ("getScale".equals(method.getName())) return scale[index];
                if ("getColumnTypeName".equals(method.getName())) return typeNames[index];
                return defaultValue(method.getReturnType());
            }
        });
    }

    ResultSet resultSet(final ResultSetMetaData metadata, final String comment, final boolean hasRow) {
        return proxy(ResultSet.class, new InvocationHandler() {
            private boolean first = true;
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("getMetaData".equals(method.getName())) return metadata;
                if ("next".equals(method.getName())) { resultSetNextCalls++; boolean result = first && hasRow; first = false; return result; }
                if ("getString".equals(method.getName())) return comment;
                return defaultValue(method.getReturnType());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }
}
