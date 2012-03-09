/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.arecibo.alert.email;

import com.google.inject.Inject;
import com.ning.arecibo.alert.guice.AlertServiceConfig;
import com.ning.arecibo.util.Logger;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class EmailManager
{
    private static final Logger log = Logger.getLogger(EmailManager.class);

    public static final int MAX_CHARS_FOR_SMS = 140;

    private final AlertServiceConfig alertServiceConfig;
    private final String sendingHostSignature;

    @Inject
    public EmailManager(final AlertServiceConfig alertServiceConfig)
    {
        this.alertServiceConfig = alertServiceConfig;

        String hostName;
        String hostIp;

        try {
            final InetAddress localHost = InetAddress.getLocalHost();
            hostName = localHost.getHostName();
            hostIp = localHost.getHostAddress();
        }
        catch (UnknownHostException uhEx) {
            log.warn("UnknownHostException: won't be able to append sending host info to outgoing email");
            log.info(uhEx);

            hostName = "";
            hostIp = "";
        }

        sendingHostSignature = String.format("\n\nSent by Arecibo Alert Service: %s (%s)", hostName, hostIp);
    }

    public boolean sendEmail(final String to, final String from, final String subject, final String message)
    {
        return sendEmail(to, from, subject, message, false);
    }

    public boolean sendEmail(final String to, final String from, final String subject, String message, final boolean appendSendingHostInfo)
    {
        if (appendSendingHostInfo) {
            message += sendingHostSignature;
        }

        try {
            final SimpleEmail email = new SimpleEmail();
            email.setHostName(alertServiceConfig.getSMTPHost());
            email.addTo(to);
            email.setFrom(from);
            email.setSubject(subject);
            email.setMsg(message);

            log.debug("Sending email to: %s, subject = '%s'", to, subject);

            email.send();

            return true;
        }
        catch (EmailException mEx) {
            log.warn("EmailException: could not send email to:'%s' from:'%s' subject:'%s'", to, from, subject);
            log.info(mEx);

            return false;
        }
    }

    public String getFrom()
    {
        return alertServiceConfig.getFromEmailAddress();
    }
}
