package org.mb6.episodenotifier;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class EpisodeNotifier {

    //    private static final String BASE_URL = "https://asd.quest/selary/مسلسل-المؤسس-عثمان/";
    private static final String BASE_URL = "https://asd.quest/مسلسل-المؤسس-عثمان-الموسم-السادس-الحل/";
    private static final String EPISODES_FILE = "episodes.txt";

    public static void main(String[] args) {
        try {
            // Step 1: Fetch the webpage
            Document doc = Jsoup.connect(BASE_URL).get();

            // Exemple de données
            //String episodeTitle = "Épisode 5 - Saison 6";
            //String episodeUrl = "https://asd.quest/مسلسل-المؤسس-عثمان-الموسم-السادس-الحل-5/";

            // Envoi des notifications
//            showDesktopNotification(episodeTitle, episodeUrl);

            // Step 2: Extract episode links from the specific div
            Elements links = doc.select("div.ContainerEpisodesList a[href]");
            Set<String> newEpisodes = new HashSet<>();

            for (Element link : links) {
                String encodedUrl = link.attr("href");
                String decodedUrl = URLDecoder.decode(encodedUrl, "UTF-8"); // Decode the URL

                newEpisodes.add(decodedUrl); // Collect the episode URL
            }

            // Step 3: Check if the episodes file exists
            File file = new File(EPISODES_FILE);
            if (!file.exists()) {
                // First run: Save all episodes as initial data
                System.out.println("First run detected. Storing initial episodes.");
                saveNewEpisodes(newEpisodes);
                System.out.println("Episodes saved. No new episodes to report.");
                return;
            }

            // Step 4: Compare with stored episodes
            Set<String> knownEpisodes = loadKnownEpisodes();
            Set<String> addedEpisodes = new HashSet<>(newEpisodes);
            addedEpisodes.removeAll(knownEpisodes);

            // Step 5: Notify and update the file
            if (!addedEpisodes.isEmpty()) {
                System.out.println("New episodes detected:");
                for (String episode : addedEpisodes) {
                    System.out.println(episode);
                    showDesktopNotification("", episode);
                }
                saveNewEpisodes(newEpisodes);

            } else {
                System.out.println("No new episodes detected.");
            }

        } catch (IOException e) {
            System.err.println("Error fetching or parsing the webpage: " + e.getMessage());
        }
    }

    // Load known episodes from the file
    private static Set<String> loadKnownEpisodes() {
        Set<String> episodes = new HashSet<>();
        try {
            Files.lines(Paths.get(EPISODES_FILE)).forEach(episodes::add);
        } catch (IOException e) {
            System.err.println("Error reading episodes file: " + e.getMessage());
        }
        return episodes;
    }

    // Save the updated list of episodes to the file
    private static void saveNewEpisodes(Set<String> episodes) {
        try (FileWriter writer = new FileWriter(new File(EPISODES_FILE))) {
            for (String episode : episodes) {
                writer.write(episode + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error saving episodes: " + e.getMessage());
        }
    }

    // Notification sur le bureau
    public static void showDesktopNotification(String episodeTitle, String episodeUrl) {
        if (!SystemTray.isSupported()) {
            System.out.println("Les notifications sur le bureau ne sont pas supportées.");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage("chapter.png"); // Remplacez par un chemin d'icône valide

            TrayIcon trayIcon = new TrayIcon(image, "Notification Nouvel Épisode");
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("Nouvel épisode détecté");
            tray.add(trayIcon);

            trayIcon.displayMessage("Nouvel Épisode Disponible",
                    "Titre : " + episodeTitle + "\nURL : " + episodeUrl,
                    TrayIcon.MessageType.INFO);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Envoi d'un e-mail
    public static void sendEmailNotification(String episodeTitle, String episodeUrl) {
        final String senderEmail = "mohamed.ambition2020@gmail.com"; // Votre adresse e-mail
        final String senderPassword = "votre-mot-de-passe"; // Mot de passe de l'e-mail
        final String recipientEmail = "destinataire@gmail.com"; // Adresse e-mail de réception

        String host = "smtp.gmail.com"; // Serveur SMTP de Gmail

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587");

        Session session = Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Nouvel épisode détecté : " + episodeTitle);
            message.setText("Un nouvel épisode a été ajouté !\n\n" +
                    "Titre : " + episodeTitle + "\n" +
                    "URL : " + episodeUrl);

            Transport.send(message);

            System.out.println("E-mail envoyé avec succès !");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
