package services.rti.metadata

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class MetadataJsonConversionsSpec extends PlaySpec with MetadataJsonConversions {

  "MetadataJsonConversions" should {
    "parse metadata from metadata json" in {
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
           |  "MaxApertureValue": "512/256"
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
        flashpixVersion = None
      )

      metadata mustBe expect
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
}
