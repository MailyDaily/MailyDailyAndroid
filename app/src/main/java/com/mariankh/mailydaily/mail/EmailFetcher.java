package com.mariankh.mailydaily.mail;

import android.os.AsyncTask;
import android.util.Log;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;

public class EmailFetcher extends AsyncTask<Void, Void, Void> {

    private String email;
    private String password;

    public EmailFetcher(String email, String password) {
        this.email = email;
        this.password = password;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            // Set properties for the IMAP server
            Properties props = new Properties();
            props.put("mail.imap.host", "imap.gmail.com");
            props.put("mail.imap.port", "993");
            props.put("mail.imap.ssl.enable", "true");
            props.put("mail.imap.auth", "true");

            // Create session
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(email, password);
                }
            });

            // Connect to the IMAP store
            Store store = session.getStore("imap");
            store.connect();

            // Access the inbox
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // Fetch unread messages
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

            for (Message message : messages) {
                MimeMessage mimeMessage = (MimeMessage) message;
                Log.d("EmailFetcher", "Subject: " + mimeMessage.getSubject());
            }

            // Close connections
            inbox.close(false);
            store.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("EmailFetcher", "Error fetching emails: " + e.getMessage());
        }

        return null;
    }
}
