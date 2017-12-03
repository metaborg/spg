package org.metaborg.spg.sentence.signature;

import java.util.Objects;

public class Sort {
    private final String name;
    private final Sort parameter;

    public Sort(String name) {
        this(name, null);
    }

    public Sort(String name, Sort parameter) {
        this.name = name;
        this.parameter = parameter;
    }

    public String getName() {
        return name;
    }

    public Sort getParameter() {
        return parameter;
    }

    @Override
    public String toString() {
        if (parameter == null) {
            return name;
        } else {
            return name + "(" + parameter + ")";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Sort sort = (Sort) o;

        return Objects.equals(name, sort.name) && Objects.equals(parameter, sort.parameter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, parameter);
    }
}
