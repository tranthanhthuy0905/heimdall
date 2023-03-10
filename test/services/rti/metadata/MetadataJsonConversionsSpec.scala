package services.rti.metadata

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, Json}
import utils.JsonFormat

class MetadataJsonConversionsSpec extends PlaySpec with MetadataJsonConversions with JsonFormat {

  "MetadataJsonConversions" should {
    "parse metadata from metadata json v1" in {
      val body =
        s"""
           |{
           |  "DateTime": "2018:10:29 10:48:06",
           |  "ImageDescription": "photo by tqmai",
           |  "Software": "Adobe Photoshop Lightroom Classic 7.5 (Macintosh)",
           |  "Orientation": "1",
           |  "ApertureValue": "6/1",
           |  "BrightnessValue": "24344/2560",
           |  "ColorSpace": "1",
           |  "Contrast": "0",
           |  "DateTimeDigitized": "2018:10:19 16:56:15",
           |  "DigitalZoomRatio": "16/16",
           |  "ExifVersion": "0231",
           |  "ExposureBiasValue": "0/10",
           |  "ExposureMode": "0",
           |  "ExposureProgram": "3",
           |  "ExposureTime":"1/320",
           |  "FNumber": "8/1",
           |  "Flash": "16",
           |  "FocalLength": "500/10",
           |  "FocalLengthIn35mmFilm": "50",
           |  "ISOSpeedRatings": "100",
           |  "LensModel": "E 50mm F2",
           |  "LensSpecification": "500/10",
           |  "LightSource": "0",
           |  "Make": "SONY",
           |  "MaxApertureValue": "512/256",
           |  "GPSLatitudeRef": "S",
           |  "GPSLatitude": "37.828308",
           |  "GPSLongitudeRef": "E",
           |  "GPSLongitude": "144.924683",
           |  "GPSAltitudeRef": "0x00",
           |  "GPSAltitude": "11.560000",
           |  "GPSTimeStamp": "12:34:56.789000",
           |  "GPSSpeedRef": "K",
           |  "GPSSpeed": "0/1",
           |  "GPSImgDirectionRef": "T",
           |  "GPSImgDirection": "600201/2530",
           |  "GPSDestBearingRef": "T",
           |  "GPSDestBearing": "600201/2530",
           |  "GPSDateStamp": "2020:03:06"
           |}
           |""".stripMargin

      val metadata = metadataFromJson(Json.parse(body))
      val expect = Metadata(
        dateTime = Some(DateTime("2018:10:29 10:48:06")),
        imageDesc = Some(ImageDescription("photo by tqmai")),
        software = Some(Software("Adobe Photoshop Lightroom Classic 7.5 (Macintosh)")),
        orientation = Some(Orientation("1")),
        pixelXDimension = None,
        pixelYDimension = None,
        xResolution = None,
        yResolution = None,
        compression = None,
        resolutionUnit = None,
        colorSpace = Some(ColorSpace("1")),
        compressedBitsPerPixel = None,
        make = Some(Make("SONY")),
        model = None,
        bodySerialNumber = None,
        fNumber = Some(FNumber("8/1")),
        exposureTime = Some(ExposureTime("1/320")),
        shutterSpeedValue = None,
        isoSpeedRatings = Some(ISOSpeedRatings("100")),
        exposureBiasValue = Some(ExposureBiasValue("0/10")),
        focalLength = Some(FocalLength("500/10")),
        focalLengthIn35mmFilm = Some(FocalLengthIn35mmFilm("50")),
        apertureValue = Some(ApertureValue("6/1")),
        maxApertureValue = Some(MaxApertureValue("512/256")),
        meteringMode = None,
        subjectDistance = None,
        flash = Some(Flash("16")),
        dateTimeOriginal = None,
        dateTimeDigitized = Some(DateTimeDigitized("2018:10:19 16:56:15")),
        lensMake = None,
        lensModel = Some(LensModel("E 50mm F2")),
        lensSerialNumber = None,
        lensSpecification = Some(LensSpecification("500/10")),
        componentsConfiguration = None,
        contrast = Some(Contrast("0")),
        brightnessValue = Some(BrightnessValue("24344/2560")),
        lightSource = Some(LightSource("0")),
        exposureProgram = Some(ExposureProgram("3")),
        exposureMode = Some(ExposureMode("0")),
        saturation = None,
        sharpness = None,
        whiteBalance = None,
        digitalZoomRatio = Some(DigitalZoomRatio("16/16")),
        sceneType = None,
        sceneCaptureType = None,
        fileSource = None,
        exifVersion = Some(ExifVersion("0231")),
        flashpixVersion = None,
        // GPS
        gpsLatitudeRef = Some(GpsLatitudeRef("S")),
        gpsLatitude = Some(GpsLatitude("37.828308")),
        gpsLongitudeRef = Some(GpsLongitudeRef("E")),
        gpsLongitude = Some(GpsLongitude("144.924683")),
        gpsAltitudeRef = Some(GpsAltitudeRef("0x00")),
        gpsAltitude = Some(GpsAltitude("11.560000")),
        gpsTimeStamp = Some(GpsTimeStamp("12:34:56.789000")),
        gpsSpeedRef = Some(GpsSpeedRef("K")),
        gpsSpeed = Some(GpsSpeed("0/1")),
        gpsImageDirectionRef = Some(GpsImageDirectionRef("T")),
        gpsImageDirection = Some(GpsImageDirection("600201/2530")),
        gpsDestBearingRef = Some(GpsDestBearingRef("T")),
        gpsDestBearing = Some(GpsDestBearing("600201/2530")),
        gpsDateStamp = Some(GpsDateStamp("2020:03:06")),
      )

      metadata mustBe expect

      val actualJsonMetada = removeNullValues(Json.toJson(metadata).as[JsObject])
      val expectedJsonMetada =
        s"""
           |{
           |  "dateTime": {
           |    "displayName": "Modified Date",
           |    "displayValue": "2018:10:29 10:48:06"
           |  },
           |  "imageDescription": {
           |    "displayName": "Image Description",
           |    "displayValue": "photo by tqmai"
           |  },
           |  "software": {
           |    "displayName": "Software",
           |    "displayValue": "Adobe Photoshop Lightroom Classic 7.5 (Macintosh)"
           |  },
           |  "orientation": {
           |    "displayName": "Orientation",
           |    "displayValue": "Horizontal"
           |  },
           |  "colorSpace": {
           |    "displayName": "Color Representation",
           |    "displayValue": "sRGB"
           |  },
           |  "make": {
           |    "displayName": "Camera Make",
           |    "displayValue": "SONY"
           |  },
           |  "fNumber": {
           |    "displayName": "F-Stop",
           |    "displayValue": "8/1"
           |  },
           |  "exposureTime": {
           |    "displayName": "Exposure Time",
           |    "displayValue": "1/320"
           |  },
           |  "isoSpeedRatings": {
           |    "displayName": "ISO Speed",
           |    "displayValue": "100"
           |  },
           |  "exposureBiasValue": {
           |    "displayName": "Exposure Bias",
           |    "displayValue": "0/10"
           |  },
           |  "focalLength": {
           |    "displayName": "Focal Length",
           |    "displayValue": "500/10"
           |  },
           |  "focalLengthIn35mmFilm": {
           |    "displayName": "35mm Focal Length",
           |    "displayValue": "50"
           |  },
           |  "apertureValue": {
           |    "displayName": "Aperture Value",
           |    "displayValue": "6/1"
           |  },
           |  "maxApertureValue": {
           |    "displayName": "Max Aperture Value",
           |    "displayValue": "512/256"
           |  },
           |  "flash": {
           |    "displayName": "Flash Mode",
           |    "displayValue": "Off, Did not fire"
           |  },
           |  "dateTimeDigitized": {
           |    "displayName": "Date/Time Original",
           |    "displayValue": "2018:10:19 16:56:15"
           |  },
           |  "lensModel": {
           |    "displayName": "Lens Model",
           |    "displayValue": "E 50mm F2"
           |  },
           |  "lensSpecification": {
           |    "displayName": "Lens Specification",
           |    "displayValue": "500/10"
           |  },
           |  "contrast": {
           |    "displayName": "Contrast",
           |    "displayValue": "Normal"
           |  },
           |  "brightnessValue": {
           |    "displayName": "Brightness",
           |    "displayValue": "24344/2560"
           |  },
           |  "lightSource": {
           |    "displayName": "Light Source",
           |    "displayValue": "Unknown"
           |  },
           |  "exposureProgram": {
           |    "displayName": "Exposure Program",
           |    "displayValue": "Aperture-priority AE"
           |  },
           |  "exposureMode": {
           |    "displayName": "Exposure Mode",
           |    "displayValue": "Auto"
           |  },
           |  "digitalZoomRatio": {
           |    "displayName": "Digital Zoom Ratio",
           |    "displayValue": "16/16"
           |  },
           |  "exifVersion": {
           |    "displayName": "Exif Version",
           |    "displayValue": "0231"
           |  },
           |  "gpsLatitudeRef": {
           |    "displayName": "Latitude Reference",
           |    "displayValue": "South"
           |  },
           |  "gpsLatitude": {
           |    "displayName": "Latitude",
           |    "displayValue": "37.828308"
           |  },
           |  "gpsLongitudeRef": {
           |    "displayName": "Longitude Reference",
           |    "displayValue": "East"
           |  },
           |  "gpsLongitude": {
           |    "displayName": "Longitude",
           |    "displayValue": "144.924683"
           |  },
           |  "gpsAltitudeRef": {
           |    "displayName": "Altitude Reference",
           |    "displayValue": "Above Sea Level"
           |  },
           |  "gpsAltitude": {
           |    "displayName": "Altitude",
           |    "displayValue": "11.560000"
           |  },
           |  "gpsTimeStamp": {
           |    "displayName": "Time Stamp",
           |    "displayValue": "12:34:56.789000"
           |  },
           |  "gpsSpeedRef": {
           |    "displayName": "Speed Reference",
           |    "displayValue": "km/h"
           |  },
           |  "gpsSpeed": {
           |    "displayName": "Speed",
           |    "displayValue": "0/1"
           |  },
           |  "gpsImageDirectionRef": {
           |    "displayName": "Image Direction Reference",
           |    "displayValue": "True North"
           |  },
           |  "gpsImageDirection": {
           |    "displayName": "Image Direction",
           |    "displayValue": "600201/2530"
           |  },
           |  "gpsDestBearingRef": {
           |    "displayName": "Destination Bearing Reference",
           |    "displayValue": "True North"
           |  },
           |  "gpsDestBearing": {
           |    "displayName": "Destination Bearing",
           |    "displayValue": "600201/2530"
           |  },
           |  "gpsDateStamp": {
           |    "displayName": "Date Stamp",
           |    "displayValue": "2020:03:06"
           |  }
           |}
           |""".stripMargin
      actualJsonMetada mustBe Json.parse(expectedJsonMetada)
    }
  }

  "return None field from null value of json" in {
    val body =
      s"""
         |{
         |  "FileSource": null,
         |  "Make": "Nikon"
         |}
         |""".stripMargin

    val metadata = metadataFromJson(Json.parse(body))

    metadata.fileSource mustBe None
    metadata.make mustBe Some(Make("Nikon"))
  }

  "should return None if the value of json field is not match string type" in {
    val body =
      s"""
         |{
         |  "ApertureValue": 6 
         |}
         |""".stripMargin

    val metadata = metadataFromJson(Json.parse(body))

    metadata.apertureValue mustBe None
  }

  "should return empty string if the value of json field can not convert to Int" in {
    val body =
      s"""
         |{
         |  "FileSource": "==Aq\"
         |}
         |""".stripMargin

    val metadata     = metadataFromJson(Json.parse(body))
    val jsonResult   = removeNullValues(Json.toJson(metadata).as[JsObject])
    val jsonExpected = Json.parse(s"""
                                     |{
                                     |  "fileSource": {
                                     |    "displayName": "File Source",
                                     |    "displayValue": ""
                                     |  }
                                     |}
                                     |""".stripMargin)

    jsonResult mustBe jsonExpected
  }

  "should correctly return value for FileSource, SceneType when they are in hex format" in {
    val body =
      s"""
         |{
         |  "FileSource": "0x00000003",
         |  "SceneType": "0x00000001"
         |}
         |""".stripMargin

    val metadata     = metadataFromJson(Json.parse(body))
    val jsonResult   = removeNullValues(Json.toJson(metadata).as[JsObject])
    val jsonExpected = Json.parse(s"""
                                     |{
                                     |  "fileSource": {
                                     |    "displayName": "File Source",
                                     |    "displayValue": "Digital Camera"
                                     |  },
                                     |  "sceneType": {
                                     |    "displayName": "Scene Type",
                                     |    "displayValue": "Directly photographed"
                                     |  }
                                     |}
                                     |""".stripMargin)
    jsonResult mustBe jsonExpected
  }

  "should correctly return value for FileSource, SceneType on dec format too" in {
    val body =
      s"""
         |{
         |  "FileSource": "3",
         |  "SceneType": "1"
         |}
         |""".stripMargin

    val metadata     = metadataFromJson(Json.parse(body))
    val jsonResult   = removeNullValues(Json.toJson(metadata).as[JsObject])
    val jsonExpected = Json.parse(s"""
                                     |{
                                     |  "fileSource": {
                                     |    "displayName": "File Source",
                                     |    "displayValue": "Digital Camera"
                                     |  },
                                     |  "sceneType": {
                                     |    "displayName": "Scene Type",
                                     |    "displayValue": "Directly photographed"
                                     |  }
                                     |}
                                     |""".stripMargin)
    jsonResult mustBe jsonExpected
  }

  "should correctly return empty string for unknown FileSource having value of 0x00000004" in {
    val body =
      s"""
         |{
         |  "FileSource": "0x00000004"
         |}
         |""".stripMargin
    val metadata = metadataFromJson(Json.parse(body))
    val jsonResult   = removeNullValues(Json.toJson(metadata).as[JsObject])
    val jsonExpected = Json.parse(s"""
                                    |{
                                    |  "fileSource": {
                                    |    "displayName": "File Source",
                                    |    "displayValue": ""
                                    |  }
                                    |}
                                    |""".stripMargin)
    jsonResult mustBe jsonExpected
  }
  
  "should correctly return value GPSAltitudeRef in both EXIF v1 and v2" in {
    val bodyV1 =
      s"""
         |{
         |  "GPSAltitudeRef": "0x00"
         |}
         |""".stripMargin

    val metadata     = metadataFromJson(Json.parse(bodyV1))
    val jsonResult   = removeNullValues(Json.toJson(metadata).as[JsObject])
    val jsonExpected = Json.parse(s"""
                                     |{
                                     |  "gpsAltitudeRef": {
                                     |    "displayName":  "Altitude Reference",
                                     |    "displayValue": "Above Sea Level"
                                     |  }
                                     |}
                                     |""".stripMargin)
    jsonResult mustBe jsonExpected

    val bodyV2 =
      s"""
         |{
         |  "GPSAltitudeRef": "00"
         |}
         |""".stripMargin
    val metadataV2   = metadataFromJson(Json.parse(bodyV2))
    val jsonResultV2 = removeNullValues(Json.toJson(metadata).as[JsObject])
    jsonResultV2 mustBe jsonExpected
  }

  "should return empty when parse bytes of ComponentsConfiguration failed" in {
    val body = 
    s"""
         |{
         |  "ComponentsConfiguration": {
         |     "ConfigurationId": 2,
         |     "ConfigurationBytes": "AQIDAA?="
         |  }
         |}
    """.stripMargin
    val metadata     = metadataFromJson(Json.parse(body))
    val jsonResult   = removeNullValues(Json.toJson(metadata).as[JsObject])
    val jsonExpected = Json.parse(s"""
                                     |{
                                     | "componentsConfiguration": {
                                     |    "displayName": "Components Configuration",
                                     |    "displayValue": ""
                                     |  }
                                     |}
                                     |""".stripMargin)
    jsonResult mustBe jsonExpected
  }

  "should parse newly upgraded metadata properly" in {
    val body =
      s"""
         |{
         |    "ApertureValue": "54823/32325",
         |    "BrightnessValue": "24183/50483",
         |    "ColorSpace": "65535",
         |    "ComponentsConfiguration": {
         |        "ConfigurationId": 2,
         |        "ConfigurationBytes": "AQIDAA=="
         |    },
         |    "DateTime": "2020:03:06 14:21:38",
         |    "DateTimeDigitized": "2020:03:06 14:21:38",
         |    "ExifVersion": "0231",
         |    "ExposureBiasValue": "0/1",
         |    "ExposureMode": "0",
         |    "ExposureProgram": "2",
         |    "ExposureTime": "1/13",
         |    "FNumber": "9/5",
         |    "Flash": "24",
         |    "FlashpixVersion": "0100",
         |    "FocalLength": "399/100",
         |    "FocalLengthIn35mmFilm": "28",
         |    "GPSAltitude": "11.560000",
         |    "GPSAltitudeRef": "01",
         |    "GPSDateStamp": "2020:03:06",
         |    "GPSDestBearing": "600201/2530",
         |    "GPSDestBearingRef": "T",
         |    "GPSImgDirection": "600201/2530",
         |    "GPSImgDirectionRef": "T",
         |    "GPSLatitude": "37.828308",
         |    "GPSLatitudeRef": "S",
         |    "GPSLongitude": "144.924683",
         |    "GPSLongitudeRef": "E",
         |    "GPSSpeed": "0/1",
         |    "GPSSpeedRef": "K",
         |    "GPSTag": "1970",
         |    "ISOSpeedRatings": "100",
         |    "LensMake": "Apple",
         |    "LensModel": "iPhone 7 back camera 3.99mm f/1.8",
         |    "LensSpecification": "399/100 399/100 9/5 9/5",
         |    "Make": "Apple",
         |    "MeteringMode": "5",
         |    "Model": "iPhone 7",
         |    "Orientation": "6",
         |    "PixelXDimension": "4032",
         |    "PixelYDimension": "3024",
         |    "ResolutionUnit": "2",
         |    "SceneCaptureType": "0",
         |    "SceneType": "0x01000000",
         |    "ShutterSpeedValue": "70954/19465",
         |    "Software": "13.3.1",
         |    "WhiteBalance": "0",
         |    "XResolution": "72/1",
         |    "YResolution": "72/1"
         |}
         |""".stripMargin

    val metadata     = metadataFromJson(Json.parse(body))
    val jsonResult   = removeNullValues(Json.toJson(metadata).as[JsObject])
    val jsonExpected = Json.parse(s"""
                                     {
                                     |    "dateTime": {
                                     |        "displayName": "Modified Date",
                                     |        "displayValue": "2020:03:06 14:21:38"
                                     |    },
                                     |    "software": {
                                     |        "displayName": "Software",
                                     |        "displayValue": "13.3.1"
                                     |    },
                                     |    "orientation": {
                                     |        "displayName": "Orientation",
                                     |        "displayValue": "Rotate 90 CW"
                                     |    },
                                     |    "pixelXDimension": {
                                     |        "displayName": "Image Width",
                                     |        "displayValue": "4032"
                                     |    },
                                     |    "pixelYDimension": {
                                     |        "displayName": "Image Height",
                                     |        "displayValue": "3024"
                                     |    },
                                     |    "xResolution": {
                                     |        "displayName": "X-Resolution",
                                     |        "displayValue": "72/1"
                                     |    },
                                     |    "yResolution": {
                                     |        "displayName": "Y-Resolution",
                                     |        "displayValue": "72/1"
                                     |    },
                                     |    "resolutionUnit": {
                                     |        "displayName": "Resolution Unit",
                                     |        "displayValue": "inches"
                                     |    },
                                     |    "colorSpace": {
                                     |        "displayName": "Color Representation",
                                     |        "displayValue": "Uncalibrated"
                                     |    },
                                     |    "make": {
                                     |        "displayName": "Camera Make",
                                     |        "displayValue": "Apple"
                                     |    },
                                     |    "model": {
                                     |        "displayName": "Camera Model",
                                     |        "displayValue": "iPhone 7"
                                     |    },
                                     |    "fNumber": {
                                     |        "displayName": "F-Stop",
                                     |        "displayValue": "9/5"
                                     |    },
                                     |    "exposureTime": {
                                     |        "displayName": "Exposure Time",
                                     |        "displayValue": "1/13"
                                     |    },
                                     |    "shutterSpeedValue": {
                                     |        "displayName": "Shutter Speed",
                                     |        "displayValue": "70954/19465"
                                     |    },
                                     |    "isoSpeedRatings": {
                                     |        "displayName": "ISO Speed",
                                     |        "displayValue": "100"
                                     |    },
                                     |    "exposureBiasValue": {
                                     |        "displayName": "Exposure Bias",
                                     |        "displayValue": "0/1"
                                     |    },
                                     |    "focalLength": {
                                     |        "displayName": "Focal Length",
                                     |        "displayValue": "399/100"
                                     |    },
                                     |    "focalLengthIn35mmFilm": {
                                     |        "displayName": "35mm Focal Length",
                                     |        "displayValue": "28"
                                     |    },
                                     |    "apertureValue": {
                                     |        "displayName": "Aperture Value",
                                     |        "displayValue": "54823/32325"
                                     |    },
                                     |    "meteringMode": {
                                     |        "displayName": "Metering Mode",
                                     |        "displayValue": "Multi-segment"
                                     |    },
                                     |    "flash": {
                                     |        "displayName": "Flash Mode",
                                     |        "displayValue": "Auto, Did not fire"
                                     |    },
                                     |    "dateTimeDigitized": {
                                     |        "displayName": "Date/Time Original",
                                     |        "displayValue": "2020:03:06 14:21:38"
                                     |    },
                                     |    "lensMake": {
                                     |        "displayName": "Lens Make",
                                     |        "displayValue": "Apple"
                                     |    },
                                     |    "lensModel": {
                                     |        "displayName": "Lens Model",
                                     |        "displayValue": "iPhone 7 back camera 3.99mm f/1.8"
                                     |    },
                                     |    "lensSpecification": {
                                     |        "displayName": "Lens Specification",
                                     |        "displayValue": "399/100 399/100 9/5 9/5"
                                     |    },
                                     |    "componentsConfiguration": {
                                     |        "displayName": "Components Configuration",
                                     |        "displayValue": "YCbCr"
                                     |    },
                                     |    "brightnessValue": {
                                     |        "displayName": "Brightness",
                                     |        "displayValue": "24183/50483"
                                     |    },
                                     |    "exposureProgram": {
                                     |        "displayName": "Exposure Program",
                                     |        "displayValue": "Program AE"
                                     |    },
                                     |    "exposureMode": {
                                     |        "displayName": "Exposure Mode",
                                     |        "displayValue": "Auto"
                                     |    },
                                     |    "whiteBalance": {
                                     |        "displayName": "White Balance Mode",
                                     |        "displayValue": "Auto"
                                     |    },
                                     |    "sceneType": {
                                     |        "displayName": "Scene Type",
                                     |        "displayValue": ""
                                     |    },
                                     |    "sceneCaptureType": {
                                     |        "displayName": "Scene Capture Type",
                                     |        "displayValue": "Standard"
                                     |    },
                                     |    "exifVersion": {
                                     |        "displayName": "Exif Version",
                                     |        "displayValue": "0231"
                                     |    },
                                     |    "flashpixVersion": {
                                     |        "displayName": "Flashpix Version",
                                     |        "displayValue": "0100"
                                     |    },
                                     |    "gpsLatitudeRef": {
                                     |        "displayName": "Latitude Reference",
                                     |        "displayValue": "South"
                                     |    },
                                     |    "gpsLatitude": {
                                     |        "displayName": "Latitude",
                                     |        "displayValue": "37.828308"
                                     |    },
                                     |    "gpsLongitudeRef": {
                                     |        "displayName": "Longitude Reference",
                                     |        "displayValue": "East"
                                     |    },
                                     |    "gpsLongitude": {
                                     |        "displayName": "Longitude",
                                     |        "displayValue": "144.924683"
                                     |    },
                                     |    "gpsAltitudeRef": {
                                     |        "displayName": "Altitude Reference",
                                     |        "displayValue": "Below Sea Level"
                                     |    },
                                     |    "gpsAltitude": {
                                     |        "displayName": "Altitude",
                                     |        "displayValue": "11.560000"
                                     |    },
                                     |    "gpsSpeedRef": {
                                     |        "displayName": "Speed Reference",
                                     |        "displayValue": "km/h"
                                     |    },
                                     |    "gpsSpeed": {
                                     |        "displayName": "Speed",
                                     |        "displayValue": "0/1"
                                     |    },
                                     |    "gpsImageDirectionRef": {
                                     |        "displayName": "Image Direction Reference",
                                     |        "displayValue": "True North"
                                     |    },
                                     |    "gpsImageDirection": {
                                     |        "displayName": "Image Direction",
                                     |        "displayValue": "600201/2530"
                                     |    },
                                     |    "gpsDestBearingRef": {
                                     |        "displayName": "Destination Bearing Reference",
                                     |        "displayValue": "True North"
                                     |    },
                                     |    "gpsDestBearing": {
                                     |        "displayName": "Destination Bearing",
                                     |        "displayValue": "600201/2530"
                                     |    },
                                     |    "gpsDateStamp": {
                                     |        "displayName": "Date Stamp",
                                     |        "displayValue": "2020:03:06"
                                     |    }
                                     |}
                                     |""".stripMargin)

    jsonResult mustBe jsonExpected
  }
}
