package ormSpring.services;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ormSpring.dto.InformationDto;
import ormSpring.models.Document;
import ormSpring.models.User;
import ormSpring.repositories.DocumentsRepository;
import ormSpring.repositories.UsersRepository;


import javax.imageio.ImageIO;
import javax.transaction.Transactional;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    private final static String FILES_PATH = "files";
    private final static File FILE_PATH_HELPER = new File("D:\\HomeworkWithORM\\src\\main\\resources\\");
    private final static String CONVERTED_FILES_PATH = "converted_files";

    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private DocumentsRepository documentsRepository;

    @Override
    public void init() {
        User admin = usersRepository.getOne(1L);

        try (Stream<Path> filesPaths = Files.walk(Paths.get(
                new URI(FILE_PATH_HELPER.toURI().toString() + FILES_PATH)
        ))) {
            filesPaths.filter(filePath -> filePath.toFile().isFile()).forEach(
                    filePath -> {
                        File file = filePath.toFile();
                        Document document = null;
                        try {
                            document = Document.builder()
                                    .owner(admin)
                                    .path(filePath.toString())
                                    .size(file.length())
                                    .type(Files.probeContentType(filePath))
                                    .build();
                        } catch (IOException e) {
                            throw new IllegalArgumentException(e);
                        }
                        documentsRepository.save(document);
                    }
            );
        } catch (IOException | URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Transactional
    @Override
    public void convert() {
        List<Document> documents = documentsRepository.findAll();

        for (Document document : documents) {
            String newFileName = FILE_PATH_HELPER.getPath() + "\\" + CONVERTED_FILES_PATH + "/" + document.getFileName() + ".jpg";

            if (document.getType().equals("application/pdf")) {
                convertPdfToJpg(document, newFileName);
                document.setType("image/jpeg");
            }
        }
    }

    @Transactional
    @Override
    public void convertPngByUser(Long userId) {
        User admin = usersRepository.getOne(userId);
        List<Document> documents = admin.getPngDocuments();

        for (Document document : documents) {
            String newFileName = FILE_PATH_HELPER.getPath() + "\\" + CONVERTED_FILES_PATH + "/" + document.getFileName() + ".jpg";

            convertPngToJpg(document, newFileName);
            document.setType("image/jpeg");
        }

        log.info("Get png - " + documents.size());
    }

    @Override
    public InformationDto getInformation(Long userId) {
        return usersRepository.getInformationByUser(userId);
    }

    @SneakyThrows
    private void convertPngToJpg(Document document, String newFileName) {
        BufferedImage image = ImageIO.read(document.getSourceFile());
        BufferedImage result = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_RGB);

        result.createGraphics().drawImage(image, 0, 0, Color.PINK, null);
        File resultFile = new File(newFileName);
        ImageIO.write(result, "jpg", resultFile);
        document.setSourceFile(resultFile);
    }

    private void convertPdfToJpg(Document document, String newFileName) {
        try (PDDocument pdf = PDDocument.load(document.getSourceFile())) {
            PDFRenderer renderer = new PDFRenderer(pdf);
            BufferedImage image = renderer.renderImageWithDPI(0, 300, ImageType.RGB);
            ImageIOUtil.writeImage(image, newFileName, 300);
            pdf.close();
            File resultFile = new File(newFileName);
            document.setSourceFile(resultFile);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
