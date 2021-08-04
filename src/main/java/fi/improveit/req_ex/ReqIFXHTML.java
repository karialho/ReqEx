/*
 * Copyright 2021. ImproveIt Oy
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package fi.improveit.req_ex;

import com.starbase.caliber.ImageManager;
import com.starbase.caliber.server.RemoteServerException;
import com.starbase.caliber.util.ImageHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static fi.improveit.req_ex.RoundTripGUI.theGUI;

public class ReqIFXHTML {

    private static final Logger logger = LoggerFactory.getLogger("ReqIFXHTML");

    static final String[] allowedTags = {
            "abbr", "acronym", "address", "blockquote", "br", "cite", "code",
            "dfn", "em", "kbd", "p", "pre", "q",
            "samp", "span", "strong", "var",                        // Text Module
            "dl", "dt", "dd", "ol", "ul", "li",                     // List Module
            "a",                                                    // Hypertext Module
            "del", "ins",                                           // Edit Module
            "b", "big", "hr", "i", "small", "sub", "sup", "tt",     // Presentation Module
            "caption", "table", "td", "th", "tr",                   // Basic Tables Module
            "object", "param"                                       // Object Module
    };

    static final String[] bannedTags = {
            "font", "div", "colgroup", "col", "tbody"
    };

    protected static String htmlToXhtml(ReqIFExportType exp, final String html) throws RemoteServerException {
        final Document doc = Jsoup.parse(html);

        // Select just everything within <body>
        Elements body = doc.select("body");

        // Convert unsupported <u> to <span style="text-decoration:underline">
        Elements u = body.select("u");
        u.tagName("reqif-xhtml:span");
        u.attr("style", "text-decoration:underline");

        // Convert unsupported <strike> to <span style="text-decoration:line-through">
        Elements strike = body.select("strike");
        strike.tagName("reqif-xhtml:span");
        strike.attr("style", "text-decoration:line-through");

        // Convert image to object
        Elements imgs = body.select("img");
        if (!imgs.isEmpty())
            fixImages(exp, imgs);

        // Convert <font color="xxxxxx"> to <span style="color:xxxxxx">
        // Convert <font face="yyyyyy"> to <span style="font-family:yyyyyy">
        Elements font = body.select("font");
        for (Element e : font) {
            Attributes attrs = e.attributes();
            StringBuilder style = new StringBuilder();
            for (Attribute a : attrs) {
                switch (a.getKey()) {
                    case "color":
                        String color = a.getValue();
                        style.append("color:").append(color).append(";");
                        break;
                    case "size":
                        String size = a.getValue();
                        style.append("font-size:").append(size).append(";");
                        break;
                    case "face":
                        String face = a.getValue();
                        style.append("font-family:").append(face).append(";");
                        break;
                    case "style":
                        String oldStyle = a.getValue();
                        style.append(oldStyle.toLowerCase()).append("; ");
                        break;
                }
            }
            attrs.remove("color");
            attrs.remove("size");
            attrs.remove("face");
            attrs.remove("lang");
            attrs.remove("style");
            attrs.add("style", style.toString());
            e.tagName("reqif-xhtml:span");
        }

        // Remove banned tags
        for (String tag : bannedTags) {
            Elements e = body.select(tag);
            e.unwrap();
        }

        // Convert bordercolor to style attribute in <table>
        Elements table = body.select("table");
        for (Element e : table) {
            Attributes attrs = e.attributes();
            StringBuilder style = new StringBuilder();
            for (Attribute a : attrs) {
                switch (a.getKey()) {
                    case "bordercolor":
                        String bordercolor = a.getValue();
                        style.append("border-color:").append(bordercolor).append(";");
                        break;
                    case "style":
                        String oldStyle = a.getValue();
                        style.append(oldStyle.toLowerCase()).append("; ");
                        break;
                }
            }
            attrs.remove("bordercolor");
            attrs.remove("style");
            attrs.add("style", style.toString());
        }

        // Remove illegal attributes from tags
        Elements p = body.select("p");
        p.removeAttr("align");
        Elements td = body.select("td");
        td.removeAttr("width");
        td.removeAttr("nowrap");
        Elements ul = body.select("ul");
        ul.removeAttr("type");
        Elements li = body.select("li");
        li.removeAttr("align");
        Elements blockquote = body.select("blockquote");
        blockquote.removeAttr("dir");

        // Add reqif-xhtml: namespace to all supported tags
        for (String tag : allowedTags) {
            Elements e = body.select(tag);
            e.tagName("reqif-xhtml:" + tag);
        }

        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        doc.outputSettings().escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);

        return body.html();
    }

    // Convert unsupported <img> to <object>
    // Save the image to file system
    private static void fixImages(ReqIFExportType exp, Elements imgs) throws RemoteServerException {
        logger.info("Handling images in html.");
        ImageManager im = theGUI.cs.getSession().getImageManager();
        for (Element e : imgs) {
            String imageIDS = "";
            String fileName = null;
            int imageID = 0;
            Attributes attrs = e.attributes();
            for (Attribute a : attrs) {
                switch (a.getKey()) {
                    case "id":
                        imageIDS = a.getValue();
                        imageID = Integer.parseInt(imageIDS.substring(3));
                        break;
                    case "src_original":
                        fileName = a.getValue();
                        break;
                    default:
                        break;
                }
            }
            if (fileName == null) {
                logger.error("Image with no src_original attribute: {}", imageIDS);
                break;
            }
            String ext = ImageHelper.getExtension(fileName);
            String imgFileName;
            String type = "image/" + ext;
            if (exp.ep.isSkipImages()) {
                imgFileName = fileName;
            }
            else {
                // Find out if img type is png. Convert compatible formats.
                // Otherwise remove img tag content and exit.
                switch (ext) {
                    case "png":
                        im.populateCache(imageID);
                        imgFileName = fileName;
                        break;
                    case "jpg":
                    case "jpeg":
                    case "gif":
                    case "bmp":
                        im.populateCache(imageID);
                        logger.info("Converting image to png: {}", fileName);
                        imgFileName = fileName.substring(0, fileName.length() - ext.length()) + "png";
                        try {
                            if (convertToPNG(exp.outputDirectory + "\\" + fileName,
                                    exp.outputDirectory + "\\" + imgFileName)) {
                                logger.info("Converted image to png: {}", fileName);
                            } else {
                                logger.error("Could not convert image: {}", fileName);
                                e.unwrap();
                                return;
                            }
                        } catch (IOException ex) {
                            logger.error("Error during image conversion.", ex);
                            e.unwrap();
                            return;
                        }
                        break;
                    default:
                        logger.error("Incompatible image format: {}", fileName);
                        e.unwrap();
                        return;
                }
                // Add image to the set of files to be zipped.
                exp.exportFiles.add(imgFileName);
                logger.info("Added image file: {}", imgFileName);
            }
            attrs.remove("align");
            attrs.remove("alt");
            attrs.remove("border");
            attrs.remove("height");
            attrs.remove("width");
            attrs.remove("hspace");
            attrs.remove("vspace");
            attrs.remove("id");
            attrs.remove("src");
            attrs.remove("src_original");
            attrs.add("data", imgFileName);
            attrs.add("type", type);
            e.tagName("reqif-xhtml:object");
        }
    }

    private static boolean convertToPNG(String inputImagePath,
                                        String outputImagePath) throws IOException {
        FileInputStream inputStream = new FileInputStream(inputImagePath);
        FileOutputStream outputStream = new FileOutputStream(outputImagePath);

        // reads input image from file
        BufferedImage inputImage = ImageIO.read(inputStream);

        // writes to the output image in specified format
        boolean result = ImageIO.write(inputImage, "PNG", outputStream);

        // needs to close the streams
        outputStream.close();
        inputStream.close();

        return result;

    }

}
