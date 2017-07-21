import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.util.PdfUtilities;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.junit.Assert;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by robert.drysdale on 21/07/2017.
 *
 * Run prepare_pdfs.sh before running this test
 */
public class Tess4jTest {

    @Test
    public void testGhostscriptVsPdfBox() throws Exception {
        File dir = new File("target/ocr/pdfs/");
        for (File pdf : dir.listFiles()) {
            long start = System.currentTimeMillis();
            File[] gsFiles = PdfUtilities.convertPdf2Png(pdf);
            long gsFinish = System.currentTimeMillis();
            File[] pdfBoxFiles = convertPdf2PngUsingPDFBox(pdf);
            long pdfBoxFinish = System.currentTimeMillis();

            System.out.println("---- PDF: " + pdf + " ---");
            System.out.println("GS Duration: " + (gsFinish - start) + " ms");
            System.out.println("PDFBox Duration: " + (pdfBoxFinish - gsFinish) + " ms");

            for (int i = 0; i < gsFiles.length; i++) {
                File gsFile = gsFiles[i];
                File pdfBoxFile = pdfBoxFiles[i];

                ITesseract instance = new Tesseract();  // JNA Interface Mapping
                instance.setDatapath("/usr/local/Cellar/tesseract/3.05.01/share/");
                // ITesseract instance = new Tesseract1(); // JNA Direct Mapping

                String gsText = instance.doOCR(gsFile);
                String pdfBoxText = instance.doOCR(pdfBoxFile);


                Assert.assertEquals("PDF " + pdf + " not equal", gsText, pdfBoxText);

                File txtFile = new File(pdf.getParentFile().getParentFile().getAbsolutePath() + "/text/" +
                        pdf.getName() + "." +
                        gsFile.getName().replaceAll("[^0-9]", "") + ".gs.txt");
                FileWriter writer = new FileWriter(txtFile);
                writer.write(gsText);
                writer.close();

                txtFile = new File(pdf.getParentFile().getParentFile().getAbsolutePath() + "/text/" +
                        pdf.getName() + "." +
                        pdfBoxFile.getName().replaceAll("[^0-9]", "") + ".pdfbox.txt");
                writer = new FileWriter(txtFile);
                writer.write(pdfBoxText);
                writer.close();
            }

            File[] pngs = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith("png");
                }
            });

            for (File png : pngs) {
                png.delete();
            }
        }
    }

    public static File[] convertPdf2PngUsingPDFBox(File inputPdfFile)  throws Exception {
        File imageDir = inputPdfFile.getParentFile();

        if (imageDir == null) {
            String userDir = System.getProperty("user.dir");
            imageDir = new File(userDir);
        }

        PDDocument document = null;
        try {
            document = PDDocument.load(inputPdfFile);
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); ++page) {
                BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);

                // suffix in filename will be used as the file format
                String filename = String.format("pdfbox-workingimage%03d.png", page + 1);
                ImageIOUtil.writeImage(bim, new File(imageDir, filename).getAbsolutePath(), 300);
            }
        }
        finally {
            if (document != null) {
                try {
                    document.close();
                }
                catch (Exception e) {}
            }
        }

        // find working files
        File[] workingFiles = imageDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().matches("pdfbox-workingimage\\d{3}\\.png$");
            }
        });

        Arrays.sort(workingFiles, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });

        return workingFiles;
    }
}
