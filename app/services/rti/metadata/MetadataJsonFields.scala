package services.rti.metadata

import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.util.Try

trait MetadataJsonFields {
  protected final val displayNameFieldOutput  = "displayName"
  protected final val displayValueFieldOutput = "displayValue"

  protected final val dateTimeFieldInput                = "DateTime"
  protected final val imageDescriptionFieldInput        = "ImageDescription"
  protected final val softwareFieldInput                = "Software"
  protected final val orientationFieldInput             = "Orientation"
  protected final val pixelXDimensionFieldInput         = "PixelXDimension"
  protected final val pixelYDimensionFieldInput         = "PixelYDimension"
  protected final val xResolutionFieldInput             = "XResolution"
  protected final val yResolutionFieldInput             = "YResolution"
  protected final val compressionFieldInput             = "Compression"
  protected final val resolutionUnitFieldInput          = "ResolutionUnit"
  protected final val colorSpaceFieldInput              = "ColorSpace"
  protected final val compressedBitsPerPixelFieldInput  = "CompressedBitsPerPixel"
  protected final val makeFieldInput                    = "Make"
  protected final val modelFieldInput                   = "Model"
  protected final val bodySerialNumberFieldInput        = "BodySerialNumber"
  protected final val fNumberFieldInput                 = "FNumber"
  protected final val exposureTimeFieldInput            = "ExposureTime"
  protected final val shutterSpeedValueFieldInput       = "ShutterSpeedValue"
  protected final val isoSpeedRatingsFieldInput         = "ISOSpeedRatings"
  protected final val exposureBiasValueFieldInput       = "ExposureBiasValue"
  protected final val focalLengthFieldInput             = "FocalLength"
  protected final val focalLengthIn35mmFilmFieldInput   = "FocalLengthIn35mmFilm"
  protected final val apertureValueFieldInput           = "ApertureValue"
  protected final val maxApertureValueFieldInput        = "MaxApertureValue"
  protected final val meteringModeFieldInput            = "MeteringMode"
  protected final val subjectDistanceFieldInput         = "SubjectDistance"
  protected final val flashFieldInput                   = "Flash"
  protected final val dateTimeOriginalFieldInput        = "DateTimeOriginal"
  protected final val dateTimeDigitizedFieldInput       = "DateTimeDigitized"
  protected final val lensMakeFieldInput                = "LensMake"
  protected final val lensModelFieldInput               = "LensModel"
  protected final val lensSerialNumberFieldInput        = "LensSerialNumber"
  protected final val lensSpecificationFieldInput       = "LensSpecification"
  protected final val componentsConfigurationFieldInput = "ComponentsConfiguration"
  protected final val configurationBytesFieldInput      = "ConfigurationBytes"
  protected final val contrastFieldInput                = "Contrast"
  protected final val brightnessValueFieldInput         = "BrightnessValue"
  protected final val lightSourceFieldInput             = "LightSource"
  protected final val exposureProgramFieldInput         = "ExposureProgram"
  protected final val exposureModeFieldInput            = "ExposureMode"
  protected final val saturationFieldInput              = "Saturation"
  protected final val sharpnessFieldInput               = "Sharpness"
  protected final val whiteBalanceFieldInput            = "WhiteBalance"
  protected final val digitalZoomRatioFieldInput        = "DigitalZoomRatio"
  protected final val sceneTypeFieldInput               = "SceneType"
  protected final val sceneCaptureTypeFieldInput        = "SceneCaptureType"
  protected final val fileSourceFieldInput              = "FileSource"
  protected final val exifVersionFieldInput             = "ExifVersion"
  protected final val flashpixVersionFieldInput         = "FlashpixVersion"
  // GPS
  protected final val gpsLatitudeRefInput                = "GPSLatitudeRef"
  protected final val gpsLatitudeInput                   = "GPSLatitude"
  protected final val gpsLongitudeRefInput               = "GPSLongitudeRef"
  protected final val gpsLongitudeInput                  = "GPSLongitude"
  protected final val gpsAltitudeRefInput                = "GPSAltitudeRef"
  protected final val gpsAltitudeInput                   = "GPSAltitude"
  protected final val gpsTimeStampInput                  = "GPSTimeStamp"
  protected final val gpsSpeedRefInput                   = "GPSSpeedRef"
  protected final val gpsSpeedInput                      = "GPSSpeed"
  protected final val gpsImageDirectionRefInput          = "GPSImgDirectionRef"
  protected final val gpsImageDirectionInput             = "GPSImgDirection"
  protected final val gpsDestBearingRefInput             = "GPSDestBearingRef"
  protected final val gpsDestBearingInput                = "GPSDestBearing"
  protected final val gpsDateStampInput                  = "GPSDateStamp"

  protected final val dateTimeFieldOutput                = "dateTime"
  protected final val imageDescriptionFieldOutput        = "imageDescription"
  protected final val softwareFieldOutput                = "software"
  protected final val orientationFieldOutput             = "orientation"
  protected final val pixelXDimensionFieldOutput         = "pixelXDimension"
  protected final val pixelYDimensionFieldOutput         = "pixelYDimension"
  protected final val xResolutionFieldOutput             = "xResolution"
  protected final val yResolutionFieldOutput             = "yResolution"
  protected final val compressionFieldOutput             = "compression"
  protected final val resolutionUnitFieldOutput          = "resolutionUnit"
  protected final val colorSpaceFieldOutput              = "colorSpace"
  protected final val compressedBitsPerPixelFieldOutput  = "compressedBitsPerPixel"
  protected final val makeFieldOutput                    = "make"
  protected final val modelFieldOutput                   = "model"
  protected final val bodySerialNumberFieldOutput        = "bodySerialNumber"
  protected final val fNumberFieldOutput                 = "fNumber"
  protected final val exposureTimeFieldOutput            = "exposureTime"
  protected final val shutterSpeedValueFieldOutput       = "shutterSpeedValue"
  protected final val isoSpeedRatingsFieldOutput         = "isoSpeedRatings"
  protected final val exposureBiasValueFieldOutput       = "exposureBiasValue"
  protected final val focalLengthFieldOutput             = "focalLength"
  protected final val focalLengthIn35mmFilmFieldOutput   = "focalLengthIn35mmFilm"
  protected final val apertureValueFieldOutput           = "apertureValue"
  protected final val maxApertureValueFieldOutput        = "maxApertureValue"
  protected final val meteringModeFieldOutput            = "meteringMode"
  protected final val subjectDistanceFieldOutput         = "subjectDistance"
  protected final val flashFieldOutput                   = "flash"
  protected final val dateTimeOriginalFieldOutput        = "dateTimeOriginal"
  protected final val dateTimeDigitizedFieldOutput       = "dateTimeDigitized"
  protected final val lensMakeFieldOutput                = "lensMake"
  protected final val lensModelFieldOutput               = "lensModel"
  protected final val lensSerialNumberFieldOutput        = "lensSerialNumber"
  protected final val lensSpecificationFieldOutput       = "lensSpecification"
  protected final val componentsConfigurationFieldOutput = "componentsConfiguration"
  protected final val contrastFieldOutput                = "contrast"
  protected final val brightnessValueFieldOutput         = "brightnessValue"
  protected final val lightSourceFieldOutput             = "lightSource"
  protected final val exposureProgramFieldOutput         = "exposureProgram"
  protected final val exposureModeFieldOutput            = "exposureMode"
  protected final val saturationFieldOutput              = "saturation"
  protected final val sharpnessFieldOutput               = "sharpness"
  protected final val whiteBalanceFieldOutput            = "whiteBalance"
  protected final val digitalZoomRatioFieldOutput        = "digitalZoomRatio"
  protected final val sceneTypeFieldOutput               = "sceneType"
  protected final val sceneCaptureTypeFieldOutput        = "sceneCaptureType"
  protected final val fileSourceFieldOutput              = "fileSource"
  protected final val exifVersionFieldOutput             = "exifVersion"
  protected final val flashpixVersionFieldOutput         = "flashpixVersion"
  // GPS
  protected final val gpsLatitudeRefOutput                = "gpsLatitudeRef"
  protected final val gpsLatitudeOutput                   = "gpsLatitude"
  protected final val gpsLongitudeRefOutput               = "gpsLongitudeRef"
  protected final val gpsLongitudeOutput                  = "gpsLongitude"
  protected final val gpsAltitudeRefOutput                = "gpsAltitudeRef"
  protected final val gpsAltitudeOutput                   = "gpsAltitude"
  protected final val gpsTimeStampOutput                  = "gpsTimeStamp"
  protected final val gpsSpeedRefOutput                   = "gpsSpeedRef"
  protected final val gpsSpeedOutput                      = "gpsSpeed"
  protected final val gpsImageDirectionRefOutput          = "gpsImageDirectionRef"
  protected final val gpsImageDirectionOutput             = "gpsImageDirection"
  protected final val gpsDestBearingRefOutput             = "gpsDestBearingRef"
  protected final val gpsDestBearingOutput                = "gpsDestBearing"
  protected final val gpsDateStampOutput                  = "gpsDateStamp"

  protected final val writesBuilder = (
    (__ \ displayNameFieldOutput).write[String] and
      (__ \ displayValueFieldOutput).write[String]
  )
}

trait ReadableStrValue {
  protected def getMetadataValue(value: String): Option[MetadataValue]

  protected def fromString(data: String): String =
    this.getMetadataValue(data).map(_.displayValue).getOrElse("")
}

trait ReadableValue {
  protected def getMetadataValue(value: Int): Option[MetadataValue]

  protected def fromString(data: String): String =
    tryToInt(data)
      .flatMap(getMetadataValue)
      .map(_.displayValue)
      .getOrElse("")

  private def tryToInt(s: String): Option[Int] = Try(s.toInt).toOption
}

trait ReadableHexValue {
  protected def getMetadataValue(value: Int): Option[MetadataValue]

  protected def fromString(data: String): String =
    tryToInt(data)
      .flatMap(getMetadataValue)
      .map(_.displayValue)
      .getOrElse("")

  private def tryToInt(s: String): Option[Int] = 
    if (s.startsWith("0x")) 
      Try(Integer.parseInt(s.stripPrefix("0x"), 16)).toOption
    else
      Try(s.toInt).toOption
}
