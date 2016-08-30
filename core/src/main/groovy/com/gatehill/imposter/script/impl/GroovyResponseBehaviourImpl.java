package com.gatehill.imposter.script.impl;

import com.gatehill.imposter.script.ResponseBehaviourType;
import com.gatehill.imposter.script.MutableResponseBehaviour;
import groovy.lang.Script;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public abstract class GroovyResponseBehaviourImpl extends Script implements MutableResponseBehaviour {
    private final MutableResponseBehaviour delegate = new MutableResponseBehaviourImpl();

    @Override
    public ResponseBehaviourType getBehaviourType() {
        return delegate.getBehaviourType();
    }

    @Override
    public String getResponseFile() {
        return delegate.getResponseFile();
    }

    @Override
    public int getStatusCode() {
        return delegate.getStatusCode();
    }

    @Override
    public MutableResponseBehaviour withStatusCode(int statusCode) {
        delegate.withStatusCode(statusCode);
        return this;
    }

    @Override
    public MutableResponseBehaviour withFile(String responseFile) {
        delegate.withFile(responseFile);
        return this;
    }

    @Override
    public MutableResponseBehaviour withEmpty() {
        delegate.withEmpty();
        return this;
    }

    @Override
    public MutableResponseBehaviour usingDefaultBehaviour() {
        delegate.usingDefaultBehaviour();
        return this;
    }

    @Override
    public MutableResponseBehaviour immediately() {
        delegate.immediately();
        return this;
    }

    @Override
    public MutableResponseBehaviour respond() {
        delegate.respond();
        return this;
    }

    @Override
    public MutableResponseBehaviour respond(Runnable closure) {
        delegate.respond(closure);
        return this;
    }

    @Override
    public MutableResponseBehaviour and() {
        delegate.and();
        return this;
    }
}
