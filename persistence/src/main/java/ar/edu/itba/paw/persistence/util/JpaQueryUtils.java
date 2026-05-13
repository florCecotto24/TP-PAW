package ar.edu.itba.paw.persistence.util;

import java.util.Map;

import javax.persistence.Query;

public final class JpaQueryUtils {

    private JpaQueryUtils() {}

    public static Query bindParams(final Query q, final Map<String, Object> params) {
        params.forEach(q::setParameter);
        return q;
    }
}
