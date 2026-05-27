package ar.edu.itba.paw.webapp.listener;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import ar.edu.itba.paw.webapp.util.PublishCarInsuranceSessionStash;
import ar.edu.itba.paw.webapp.util.PublishCarPictureSessionStash;

/**
 * Deletes temporary files from the publish form (retained pictures and insurance document)
 * when the HTTP session expires.
 */
public final class PublishCarStashSessionListener implements HttpSessionListener {

    @Override
    public void sessionCreated(final HttpSessionEvent se) {
        
    }

    @Override
    public void sessionDestroyed(final HttpSessionEvent se) {
        final String sessionId = se.getSession().getId();
        PublishCarPictureSessionStash.deleteStashFilesForSessionId(sessionId);
        PublishCarInsuranceSessionStash.deleteStashFilesForSessionId(sessionId);
    }
}
