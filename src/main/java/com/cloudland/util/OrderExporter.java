package com.cloudland.util;

import com.cloudland.config.StorageProperties;
import com.cloudland.pojo.vo.OrderVO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class OrderExporter {
    @Resource
    private StorageProperties storageProperties;

    public void exportToExcel(List<OrderVO> orderVOS, Integer type) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDate = now.format(formatter);

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("订单");
        Row headerRow = sheet.createRow(0);
        Cell cell = headerRow.createCell(0);
        cell.setCellValue("订单ID");
        cell = headerRow.createCell(1);
        cell.setCellValue("商品名称");
        cell = headerRow.createCell(2);
        cell.setCellValue("数量");
        cell = headerRow.createCell(3);
        cell.setCellValue("价格");
        cell = headerRow.createCell(4);
        cell.setCellValue("总价");
        cell = headerRow.createCell(5);
        cell.setCellValue("订单创建时间");
        cell = headerRow.createCell(6);
        cell.setCellValue("订单支付时间");
        cell = headerRow.createCell(7);
        cell.setCellValue("订单状态");
        cell = headerRow.createCell(8);
        cell.setCellValue("下单人");
        cell = headerRow.createCell(9);
        cell.setCellValue("下单人联系电话");

        int rowNum = 1;
        for (OrderVO orderVO : orderVOS) {
            Row dataRow = sheet.createRow(rowNum++);
            cell = dataRow.createCell(0);
            cell.setCellValue(orderVO.getId());
            cell = dataRow.createCell(1);
            cell.setCellValue(orderVO.getProductName());
            cell = dataRow.createCell(2);
            cell.setCellValue(orderVO.getNum());
            cell = dataRow.createCell(3);
            cell.setCellValue(orderVO.getPrice());
            cell = dataRow.createCell(4);
            cell.setCellValue(orderVO.getPrice() * orderVO.getNum());
            cell = dataRow.createCell(5);
            String createTime = orderVO.getCreateTime().format(formatter);
            cell.setCellValue(createTime);
            cell = dataRow.createCell(6);
            String payTime = orderVO.getPayTime().format(formatter);
            cell.setCellValue(payTime);
            cell = dataRow.createCell(7);
            if (orderVO.getStatus() == 1) {
                cell.setCellValue("已支付");
            } else {
                cell.setCellValue("已退单");
            }
            cell = dataRow.createCell(8);
            cell.setCellValue(orderVO.getUsername());
            cell = dataRow.createCell(9);
            cell.setCellValue(orderVO.getPhone());
        }

        Row dataRow = sheet.createRow(rowNum + 1);
        cell = dataRow.createCell(0);
        cell.setCellValue("统计日期截止：" + formattedDate);

        try (FileOutputStream outputStream = new FileOutputStream(storageProperties.getExportPath(type))) {
            workbook.write(outputStream);
        } finally {
            workbook.close();
        }
    }
}
