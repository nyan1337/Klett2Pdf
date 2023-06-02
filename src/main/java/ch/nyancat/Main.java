package ch.nyancat;

import com.github.kevinsawicki.http.HttpRequest;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.cli.*;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Main {

    public static void main(String[] args) {
        Options options = new Options();
        Option url = new Option("u", "url", true, "The base URL of the book (https://bridge.klett.de/xxx-xxxxxxxxxx/)");
        url.setRequired(true);
        options.addOption(url);
        Option cookie = new Option("c", "cookie", true, "The cookie header that should get send to the server to authenticate. Copy this from your browser when you're logged in.");
        cookie.setRequired(true);
        options.addOption(cookie);
        Option savePath = new Option("o", "output", true, "Output path of the pdf");
        options.addOption(savePath);
        Option resume = new Option("r", "resume", false, "Tries to resume the download");
        options.addOption(resume);
        Option temps = new Option("t", "temps", false, "Keep the temporary files (png files of each page)");
        options.addOption(temps);
        Option pages = new Option("p", "pages", true, "The number of pages the book has. If auto-detection by the data.json file fails, this is required.");
        options.addOption(pages);

        HelpFormatter helper = new HelpFormatter();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            helper.printHelp("Klett2Pdf.jar [-u url] [-c cookie] [-o output] [-r resume] [-t temps] [-p pages]", options);
            System.exit(0);
        }
        String baseURL = cmd.getOptionValue("u");
        if (!baseURL.endsWith("/")) {
            baseURL += "/";
        }
        String cookieHeader = cmd.getOptionValue("c");
        String output;
        if (cmd.hasOption("o")) {
            output = cmd.getOptionValue("o");
        } else {
            // use current working directory as output path
            output = System.getProperty("user.dir");
        }
        if (!output.endsWith("/")) {
            output += "/";
        }
        int sites;
        if (cmd.hasOption("p")) {
            sites = Integer.parseInt(cmd.getOptionValue("p"));
        } else {
            sites = getSitesNumber(baseURL, cookieHeader);
            if (sites == -1) {
                System.out.println("Could not auto-detect the number of pages. Please define the number of pages with the argument -p [number of sites].");
                System.exit(0);
            }
        }
        int offset = 0;
        if (cmd.hasOption("r")) {
            System.out.println("Trying to resume download...");
            offset = getDownloadedSites(output, sites);
        }
        downloadImages(offset, sites, output, cookieHeader, baseURL);
        System.out.println("Download finished. Merging images into one pdf...");
        imagesToPdf(output, sites, "output");
        if (!cmd.hasOption("t")) {
            System.out.println("Cleaning up...");
            cleanup(output, sites);
        }
        System.out.println("Done!");
    }

    public static void downloadImages(int offset, int sites, String output, String cookie, String baseURL) {
        for (int currentSite = offset; currentSite < sites; currentSite++) {
            System.out.println("Downloading page " + currentSite + " of " + sites + ", " + (sites - currentSite - 1) + " pages left");
            File file = new File(output + "page" + currentSite + ".png");
            String requestUrl = baseURL + "content/pages/page_" + currentSite + "/Scale4.png";
            InputStream stream = HttpRequest.get(requestUrl).accept("image/avif,image/webp,*/*").acceptEncoding("gzip, deflate, br").header("Cookie", cookie).stream();
            try {
                BufferedImage image = ImageIO.read(stream);
                ImageIO.write(image, "png", file);
                System.out.println("Success!");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void imagesToPdf(String path, int sites, String fileName) {
        File file0 = new File(path + "page0.png");
        Image image0;
        try {
            image0 = Image.getInstance(file0.toURI().toURL());
        } catch (BadElementException | IOException e) {
            throw new RuntimeException(e);
        }
        Rectangle r0 = new Rectangle(image0.getPlainWidth(), image0.getPlainHeight());
        Document document = new Document(r0, 0, 0, 0, 0);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(path + fileName + ".pdf"));
            document.open();
            for (int i = 0; i < sites; i++) {
                File file = new File(path + "page" + i + ".png");
                Image image = Image.getInstance(file.toURI().toURL());
                Rectangle r = new Rectangle(image.getPlainWidth(), image.getPlainHeight());
                document.setPageSize(r);
                document.add(image);
                document.newPage();
            }
            document.close();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void cleanup(String path, int sitesNumber) {
        for (int i = 0; i < sitesNumber; i++) {
            File f = new File(path + "page" + i + ".png");
            f.delete();
        }
    }

    public static int getDownloadedSites(String path, int expectedSitesNumber) {
        for (int i = 0; i < expectedSitesNumber; i++) {
            File page = new File(path + "page" + i + ".png");
            if (!page.isFile() || !(page.length() > 0)) {
                return i;
            }
        }
        return expectedSitesNumber;
    }

    public static int getSitesNumber(String baseURL, String cookie) {
        String dataURL = baseURL + "data.json";
        HttpRequest r = new HttpRequest(dataURL, HttpRequest.METHOD_GET).header("Cookie", cookie).acceptJson();
        if (r.code() != 200) {
            return -1;
        }
        JSONObject bookData = new JSONObject(r.body());
        return bookData.getJSONArray("pages").length();
    }
}