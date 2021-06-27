package fi.improveit.req_ex;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * High level abstraction of an Excel export file for the purpose of ReqEx
 *
 * @author Kari Alho
 *
 */
public class ExcelOutFile {

    private final String directory;
    private final String project;
    private final String reqType;
    private File outputFile;
    private int columnCount;
    private int rowCount;
    private int descriptionColumn;

    private Row row = null;
    private Cell cell = null;

    private Sheet ws = null;
    private Workbook wb = null;
    private CellStyle boldStyle, wrapStyle;


    private static final String EXPORT_FILE_PREFIX = "Export - ";
    private static final String EXPORT_FILE_SUFFIX = ".xlsx";

    private static final Logger logger = LoggerFactory.getLogger(ExcelOutFile.class.getName());

    ExcelOutFile(String dir, String project, String reqType) throws ExcelFileException {
        if (dir.length() < 1) {
            throw new ExcelFileException("Directory needs to be specified");
        }
        this.directory = dir;
        SummaryPanel.outputDir = dir;
        this.project = project;
        this.reqType = reqType;
        columnCount = 0;
        rowCount = 0;
    }

    public void open() throws ExcelFileException {
        try {
            // open the physical file
            String filename;
            if (reqType == null)
                filename = EXPORT_FILE_PREFIX + project + EXPORT_FILE_SUFFIX;
            else
                filename = EXPORT_FILE_PREFIX + project + " - " + reqType + EXPORT_FILE_SUFFIX;
            SummaryPanel.outputFile = filename;
            String pathname = directory + "\\" + filename;
            logger.info("Trying to open file {}", pathname);
            outputFile = new File(pathname);
            // create the workbook and bold font
            wb = new XSSFWorkbook();

            boldStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            boldStyle.setFont(font);
            
            wrapStyle = wb.createCellStyle();
            wrapStyle.setWrapText(true);
           
        } catch (Exception e) {
            throw new ExcelFileException("Cannot open Excel file");
        }
    }

    public void close() throws ExcelFileException {
        try {
            logger.info("Closing Excel file.");
            FileOutputStream out = new FileOutputStream(outputFile);
            wb.write(out);
            wb.close();
        } catch (IOException e) {
            throw new ExcelFileException("Cannot close Excel file");
        }
    }

    // create a sheet 
    public void addSheet(String sheetName) {
        // validate sheet name, remove invalid characters
        sheetName = sheetName.replaceAll("[^a-äA-Ö0-9 _.$%&()=#]", "");
        ws = wb.createSheet(sheetName);
        logger.info("Worksheet created: {}", sheetName);
        rowCount = 0;
    }

    // close and autosize columns
    public void closeSheet() {
        for (int i = 0; i < columnCount; i++) {
            if (i == descriptionColumn) {
                ws.setColumnWidth(i, 50 * 256);
            } else {
                ws.autoSizeColumn(i);
            }
        }
    }

    public void addRow() {
        row = ws.createRow(rowCount++);
        columnCount = 0;
    }

    public void addTitleCell(String s) {
        cell = row.createCell(columnCount++);
        cell.setCellStyle(boldStyle);
        cell.setCellValue(s);
    }

    public void addCell(Object o) {
        cell = row.createCell(columnCount++);

        if (o instanceof String) {
            cell.setCellValue((String) o);
        } else if (o instanceof Integer) {
            cell.setCellValue((Integer) o);
        } else if (o instanceof java.lang.Double) {
            cell.setCellValue((Double) o);
        } else if (o instanceof java.util.Date) {
            cell.setCellValue((java.util.Date) o);
        }
    }

    public void addCell(String[] lines) {
        cell = row.createCell(columnCount++);
        StringBuilder cv = new StringBuilder();
        int lineCount = lines.length;
        int i = 0;
        for (String l : lines) {
            cv.append(l);
            if (++i < lineCount)
                cv.append("\n");
        }
        cell.setCellStyle(wrapStyle);
        cell.setCellValue(cv.toString());
    }

    public void addNameCell(String s, short indent) {
        CellStyle indentStyle = wb.createCellStyle();
        indentStyle.setWrapText(true);
        indentStyle.setIndention(indent);

        cell = row.createCell(columnCount++);
        cell.setCellStyle(indentStyle);
        cell.setCellValue(s);
    }

    public void addDescriptionCell(String s) {
        descriptionColumn = columnCount;
        cell = row.createCell(columnCount++);
        cell.setCellStyle(wrapStyle);
        cell.setCellValue(s);
    }

    public void addCellWrapStyle(String s) {
        cell = row.createCell(columnCount++);
        cell.setCellStyle(wrapStyle);
        cell.setCellValue(s);
    }

}
