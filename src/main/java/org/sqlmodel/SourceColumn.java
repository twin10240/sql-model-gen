package org.sqlmodel;

import java.util.Objects;

final class SourceColumn {
    private final String owner;
    private final String table;
    private final String column;

    SourceColumn(String owner, String table, String column) {
        this.owner = owner;
        this.table = Objects.requireNonNull(table, "table");
        this.column = Objects.requireNonNull(column, "column");
    }

    String owner() { return owner; }
    String table() { return table; }
    String column() { return column; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SourceColumn)) return false;
        SourceColumn that = (SourceColumn) other;
        return Objects.equals(owner, that.owner) && table.equals(that.table) && column.equals(that.column);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, table, column);
    }
}
