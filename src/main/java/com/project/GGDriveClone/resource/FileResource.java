package com.project.GGDriveClone.resource;

import com.project.GGDriveClone.DTO.FileDto;
import com.project.GGDriveClone.convert.FileConvert;
import com.project.GGDriveClone.entity.AccessControlEntity;
import com.project.GGDriveClone.entity.FileEntity;
import com.project.GGDriveClone.entity.UserEntity;
import com.project.GGDriveClone.security.CustomUserDetails;
import com.project.GGDriveClone.service.AccessControlService;
import com.project.GGDriveClone.service.FileService;
import com.project.GGDriveClone.service.UserService;
import com.project.GGDriveClone.util.MediaTypeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/user")
public class FileResource {

    @Value("${file.upload-dir}")
    String FILE_DIRECTORY;
    @Autowired
    private FileConvert fileConvert;
    @Autowired
    private ServletContext servletContext;
    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @Autowired
    private AccessControlService accessControlService;

    @PostMapping("/createFolder")
    public String createFolder(@RequestParam String folderName) {
        File file = new File(FILE_DIRECTORY + folderName);
        System.out.println(file.toString());
        if (!file.exists()) {
            if (file.mkdir()) {
                return "Directory is created!";
            } else {
                return "Failed to create directory!";
            }
        }
        return "Folder existed!";
    }

    //Upload new file with request param MultipartFile
    @PostMapping("/uploadFile")
    public FileDto fileUpload(@AuthenticationPrincipal CustomUserDetails currentUser,
                              @RequestBody MultipartFile file) throws IOException {

        // Update storage when add a new File
        UserEntity userEntity = userService.findUser(currentUser.getUserId());
        // Check if file < storage
        String message = "Upload successful!\n";
        if (userEntity.getStorage() + file.getSize() < userEntity.getPlan().getMax_storage()) {
            userEntity.setStorage(userEntity.getStorage() + file.getSize());
            userService.saveUser(userEntity);
        } else {
            message = "You have reach max storage! Contact us to upgrade your limit\n";
            return fileConvert.convertToFileDto(new FileEntity(currentUser.getUserId(),
                    file.getSize(),
                    file.getOriginalFilename(),
                    file.getContentType()), message);
        }

        // Create new file and store to server
        File myFile = new File(FILE_DIRECTORY + file.getOriginalFilename());
        myFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(myFile);
        fos.write(file.getBytes());
        fos.close();

        FileEntity fileEntity = fileService.addFile(currentUser.getUserId(), file.getSize(),
                file.getOriginalFilename(), file.getContentType(), myFile.getPath());

        return fileConvert.convertToFileDto(fileEntity, message);

    }

    // Download file with request param String = filename
    @GetMapping("/downloadFile")
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam String fileName) throws IOException {

        MediaType mediaType = MediaTypeUtils.getMediaTypeForFileName(this.servletContext, fileName);

        File file = new File(FILE_DIRECTORY + "/" + fileName);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + file.getName())
                .contentType(mediaType)
                .contentLength(file.length())
                .body(resource);
    }

    // Share file with other user use other_user_id and object_id
    @PostMapping("/shareFile")
    public AccessControlEntity shareFile(@RequestParam String username, @RequestParam Long oid) {
        Long uid = userService.findUser(username).getId();
        AccessControlEntity accessControlEntity = new AccessControlEntity(uid, oid);
        return accessControlService.addAccessControlEntity(accessControlEntity);
    }

    // List file created by current user
    @GetMapping("/listFile")
    public List<FileEntity> showAllFileByUserCreated(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return fileService.findFilesByUser(currentUser.getUserId());
    }

    // List file deleted by current user
    @GetMapping("/trash")
    public List<FileEntity> showAllFileDeletedByUserCreated(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return fileService.findFilesDeletedByUser(currentUser.getUserId());
    }

    // List file shared with current user
    @GetMapping("/shareWithMe")
    public List<FileEntity> showAllFileSharedByUserID(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return fileService.findFilesSharedByUser(currentUser.getUserId());
    }

    // Delete file (Move to trash)
    @DeleteMapping("/deleteFile")
    public void moveFileToTrash(@RequestParam Long oid) {
        FileEntity fileEntity = fileService.findFile(oid);
        if (fileEntity == null) {
            System.out.println("Cannot find this file with oid: " + oid + "\n");
            return;
        }
        fileService.moveToTrash(fileEntity);
    }
}