require("dotenv").config();

const express = require("express");
const bodyParser = require("body-parser");
const app = express();
const multer = require("multer");
const path = require("path");
const crypto = require("crypto");
const fs = require("fs");
var storage;



app.use(express.json({limit: "5mb"}));
app.use(express.urlencoded({limit: "5mb"}));

var form = "<!DOCTYPE HTML><html><body>" +
"<form method='post' action='/upload' enctype='multipart/form-data'>" +
"<input type='file' name='upload'/>" +
"<input type='submit' /></form>" +
"</body></html>";

// app.use(bodyParser.json({limit: "50mb"}));
app.get('/', function (req, res){
    res.writeHead(200, {'Content-Type': 'text/html' });
    res.end(form);
  
  });
  
storage = multer.diskStorage({
    destination: "./uploads/",
    filename: (req, file, cb) => {
        return crypto.pseudoRandomBytes(16, (err, raw) => {
            if(err) {
                console.log(err);
            } else {
                return cb(null, "" + (raw.toString("hex")) + (path.extname(file.originalname)));
            }
        });
    }
})


const config = require("./databaseConfig.js");

const connection = config.connection;

const PORT = process.env.PORT || 5000;

app.listen(PORT, () => {
  console.log(`Server started on port ${PORT}`);
});

// Routes

app.post("/upload", multer({
    storage: storage
}).single("upload"), (req, res) => {
    var filename = req.file.originalname;
    var uploadFilePath = path.join(req.hostname, "uploads", req.file.filename);
    console.log(uploadFilePath);
    // res.redirect("/uploads/" + req.file.filename);
    
    // let recordingName = req.body.name;
    // let recordingData = req.body.data;
    let sql = "INSERT INTO recordings SET ?", 
        values = {
            name: filename,
            filepath: uploadFilePath
        };
    connection.query(sql, values, (err, results) => {
        if(err)
            console.log(err);
    });
    return res.status(200).end();
});
app.get('/uploads/:upload', (req, res) => {
    file = req.params.upload;
    var vid = fs.readFileSync(__dirname + "/uploads/" + file);
    res.writeHead(200, {'Content-Type': 'video/mp4' });
    res.end(vid, 'binary');
    
  });

app.get("/getRecordings", (req, res) => {
    let sql = "SELECT * FROM recordings";
    connection.query(sql, (err, results) => {
        if(err)
            console.log(err);
        res.send(results);
    });
});

app.get("/getLatestRecord", (req, res) => {
    let sql = "SELECT * FROM recordings ORDER BY id DESC LIMIT 1";
    connection.query(sql, (err, results) => {
        if(err)
            console.log(err);
        res.send(results);
    }) 
})