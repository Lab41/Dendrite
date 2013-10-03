/**
 * Copyright 2013 In-Q-Tel/Lab41
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lab41.jersey.providers;

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
