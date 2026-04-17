package ar.edu.itba.paw.webapp.listener;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import ar.edu.itba.paw.webapp.util.PublishCarPictureSessionStash;

/**
 * Deletes temporary files from the publish form when the HTTP session expires.
 */
public final class PublishCarStashSessionListener implements HttpSessionListener {

    @Override
    public void sessionCreated(final HttpSessionEvent se) {
        
    }

    @Override
    public void sessionDestroyed(final HttpSessionEvent se) {
        PublishCarPictureSessionStash.deleteStashFilesForSessionId(se.getSession().getId());
    }
}
