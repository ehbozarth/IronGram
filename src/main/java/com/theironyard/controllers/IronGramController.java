package com.theironyard.controllers;

import com.theironyard.entities.Photo;
import com.theironyard.entities.User;
import com.theironyard.services.PhotoRepository;
import com.theironyard.services.UserRepository;
import com.theironyard.utils.PasswordHash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.ws.http.HTTPException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by earlbozarth on 11/17/15.
 */


@RestController
public class IronGramController {

    @Autowired
    UserRepository users;
    @Autowired
    PhotoRepository photos;

    @RequestMapping("/login")
    public User login(HttpSession session, HttpServletResponse response, String username, String password) throws Exception {
        //Do not need Model model because there is NO MUSTACHE
        User user = users.findOneByUsername(username);
        //Does user exist in database
        if(user == null){
            user = new User();
            user.username = username;
            user.password = PasswordHash.createHash(password);
            users.save(user);
        }
        else if(!PasswordHash.validatePassword(password,user.password)){
            throw new Exception("Wrong Password");
        }
        session.setAttribute("username", username);
        response.sendRedirect("/");

        return user;//return user object
    }//End of login (/login)


    @RequestMapping("/logout")
    public void logout(HttpSession session, HttpServletResponse response) throws IOException {
        session.invalidate();
        response.sendRedirect("/");
        //System.out.println("GOODBYE");
    }//End of logout (/logout)

    @RequestMapping("/user")
    public User user(HttpSession session ){
        String username = (String) session.getAttribute("username");
        if(username == null){
            return null;//return nothing
        }
        return users.findOneByUsername(username);//return user object foundByUsername(username)
    }//End of user (/user)


    @RequestMapping("/upload")
    public Photo upload(HttpSession session,
                        HttpServletResponse response,
                        String receiver,
                        @RequestParam(defaultValue = "0") long deleteTime,
                        boolean isPublic,
                        MultipartFile photo
    ) throws Exception {
        String username = (String) session.getAttribute("username");
        if(username == null){
            throw new Exception("You are not logged in");
        }
        User senderUser = users.findOneByUsername(username);
        User receiverUser = users.findOneByUsername(receiver);

        if(receiverUser == null){
            throw new Exception("Receiver Does Not Exist");
        }

        //Ask if photo is an image file or not
        if(!photo.getContentType().startsWith("image")){
            throw new Exception("Only images are allowed here");
        }



        //Save file into public folder
        //Generates name for file in public folder
        //photo.getOriginalFilename()
        //hello.png ---> photo0987098709879078hello.png
        File photoFile = File.createTempFile("photo", photo.getOriginalFilename(), new File("public"));
        FileOutputStream fos = new FileOutputStream(photoFile);
        fos.write(photo.getBytes());

        Photo p = new Photo();
        p.sender = senderUser;
        p.receiver = receiverUser;
        p.filename = photoFile.getName();
        if(deleteTime == 0){
            p.deleteTime = 10;
        }
        else {
            p.deleteTime = deleteTime;
        }
        p.isPublic = isPublic;
        photos.save(p);

        response.sendRedirect("/");

        return p;


    }//End of upload (/upload)

    @RequestMapping("/photos")
    public List<Photo> showPhotos(HttpSession session) throws Exception {
        String username = (String) session.getAttribute("username");
        if(username == null){
            throw new Exception("You are not logged in");
        }

        User user = users.findOneByUsername(username);

        List<Photo> photosList = photos.findByReceiver(user);

        for(Photo p: photosList){
            if(p.accessTime == null){
                p.accessTime = LocalDateTime.now();
                photos.save(p);
                //Do Not Need else if, if using waitToDelete(p, (int) p.deleteTime);
                //p.deleteTime needs to cast to int because deleteTime is a long
            }
            else if(p.accessTime.isBefore(LocalDateTime.now().minusSeconds(p.deleteTime))){
                photos.delete(p);
                File tempFile = new File("public", p.filename);
                tempFile.delete();
            }
        }
        return photos.findByReceiver(user);//All of the data for photos sent to us
        // (Only have sender, receiver, and filename)
    }//End of showPhotos (/photos)

    @RequestMapping("/public-photos")
    public List<Photo> publicPhotos(String username) throws Exception {

        User user = users.findOneByUsername(username);

        ArrayList<Photo> publicList = new ArrayList();
        for(Photo p : photos.findBySender(user)){
            if(p.isPublic){
                publicList.add(p);
            }
        }
        /*
        Using stream feature
        List<Photo> selectedPhotos = photos.findBySender(sender).stream()
               .filter(p1 -> p1.isPublic)
               .collect(Collectors.toList());
       return selectedPhotos;
       */
        //return photos.findByIsPublicAndSender(true, userName);
        return publicList;

    }//End of publicPhotos


    /*
    Using Threads:
    public void waitToDelete(Photo photo, int seconds){
        Thread t = new Thread(() -> {
        try{
            Thread.sleep(seconds * 1000);
            }
        catch (Exception e){
        }
        File tempFile = new File("public", photo.filename);
                tempFile.delete();
        });//End of Thread t
        t.start();
    }//End of waitToDelete()

    Using Timer:
        Timer t = new Time();
        t.schedule(new TimeTask()){
            @Override
            public void run(){
                photos.delete(photo);
                File tempFile = new File("public", photo.filename);
                tempFile.delete();
            }
        }, seconds * 1000);

     */

}//End of Controller
