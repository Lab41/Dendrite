package org.lab41.jersey.providers;

/**
 * Created with IntelliJ IDEA.
 * User: kramachandran-admin
 * Date: 8/4/13
 * Time: 7:49 PM
 * To change this template use File | Settings | File Templates.
 */

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;
import com.tinkerpop.rexster.server.RexsterApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Type;

@Provider
@Service
public class RexsterApplicationProvider extends AbstractHttpContextInjectable<RexsterApplication>
        implements InjectableProvider<Context, Type> {

    private RexsterApplication rexsterApplication;


    /**
     * This constructor allows me to inject
     */
    @Autowired(required = true)
    public RexsterApplicationProvider(RexsterApplication rexsterApplication) {
        this.rexsterApplication = rexsterApplication;
    }


    @Override
    public RexsterApplication getValue(HttpContext c) {
        return rexsterApplication;
    }

    @Override
    public RexsterApplication getValue() {
        return rexsterApplication;
    }

    @Override
    public ComponentScope getScope() {
        return ComponentScope.Singleton;
    }

    @Override
    public Injectable getInjectable(ComponentContext ic, Context context, Type type) {
        if (type.equals(RexsterApplication.class)) {
            return this;
        }
        return null;
    }
}
