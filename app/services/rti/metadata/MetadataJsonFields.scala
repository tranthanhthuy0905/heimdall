package services.rti.metadata

import play.api.libs.functional.syntax._
import play.api.libs.json._

trait MetadataJsonFields {
  protected final val displayNameFieldCamelCase  = "displayName"
  protected final val displayValueFieldCamelCase = "displayValue"

  protected final val dateTimeField               = "DateTime"
  protected final val imageDescriptionField       = "ImageDescription"
  protected final val softwareField               = "Software"
  protected final val orientationField            = "Orientation"
  protected final val pixelXDimensionField        = "PixelXDimension"
  protected final val pixelYDimensionField        = "PixelYDimension"
  protected final val xResolutionField            = "XResolution"
  protected final val yResolutionField            = "YResolution"
  protected final val compressionField            = "Compression"
  protected final val resolutionUnitField         = "ResolutionUnit"
  protected final val colorSpaceField             = "ColorSpace"
  protected final val compressedBitsPerPixelField = "CompressedBitsPerPixel"
  protected final val makeField                   = "Make"
  protected final val modelField                  = "Model"
  protected final val bodySerialNumberField       = "BodySerialNumber"
  protected final val fNumberField                = "FNumber"
  protected final val exposureTimeField           = "ExposureTime"
  protected final val shutterSpeedValueField      = "ShutterSpeedValue"
  protected final val isoSpeedRatingsField        = "ISOSpeedRatings"
  protected final val exposureBiasValueField      = "ExposureBiasValue"
  protected final val focalLengthField            = "FocalLength"
  protected final val focalLengthIn35mmFilmField  = "FocalLengthIn35mmFilm"
  protected final val apertureValueField          = "ApertureValue"
  protected final val maxApertureValueField       = "MaxApertureValue"
  protected final val meteringModeField           = "MeteringMode"
  protected final val subjectDistanceField        = "SubjectDistance"
  protected final val flashField                  = "Flash"
  protected final val dateTimeOriginalField       = "DateTimeOriginal"
  protected final val dateTimeDigitizedField      = "DateTimeDigitized"

  protected final val dateTimeFieldCamelCase               = "dateTime"
  protected final val imageDescriptionFieldCamelCase       = "imageDescription"
  protected final val softwareFieldCamelCase               = "software"
  protected final val pixelXDimensionFieldCamelCase        = "pixelXDimension"
  protected final val pixelYDimensionFieldCamelCase        = "pixelYDimension"
  protected final val orientationFieldCamelCase            = "orientation"
  protected final val xResolutionFieldCamelCase            = "xResolution"
  protected final val yResolutionFieldCamelCase            = "yResolution"
  protected final val compressionFieldCamelCase            = "compression"
  protected final val resolutionUnitFieldCamelCase         = "resolutionUnit"
  protected final val colorSpaceFieldCamelCase             = "colorSpace"
  protected final val compressedBitsPerPixelFieldCamelCase = "compressedBitsPerPixel"
  protected final val makeFieldCamelCase                   = "make"
  protected final val modelFieldCamelCase                  = "model"
  protected final val bodySerialNumberFieldCamelCase       = "bodySerialNumber"
  protected final val fNumberFieldCamelCase                = "fNumber"
  protected final val exposureTimeFieldCamelCase           = "exposureTime"
  protected final val shutterSpeedValueFieldCamelCase      = "shutterSpeedValue"
  protected final val isoSpeedRatingsFieldCamelCase        = "isoSpeedRatings"
  protected final val exposureBiasValueFieldCamelCase      = "exposureBiasValue"
  protected final val focalLengthFieldCamelCase            = "focalLength"
  protected final val focalLengthIn35mmFilmFieldCamelCase  = "focalLengthIn35mmFilm"
  protected final val apertureValueFieldCamelCase          = "apertureValue"
  protected final val maxApertureValueFieldCamelCase       = "maxApertureValue"
  protected final val meteringModeFieldCamelCase           = "meteringMode"
  protected final val subjectDistanceFieldCamelCase        = "subjectDistance"
  protected final val flashFieldCamelCase                  = "flash"
  protected final val dateTimeOriginalFieldCamelCase       = "dateTimeOriginal"
  protected final val dateTimeDigitizedFieldCamelCase      = "dateTimeDigitized"

  protected final val writesBuilder = (
    (__ \ displayNameFieldCamelCase).write[String] and
      (__ \ displayValueFieldCamelCase).write[String]
  )
}

trait ReadableValue {
  protected def getMetadataValue(value: Int): Option[MetadataValue]

  protected def fromString(data: String): String =
    this.getMetadataValue(data.toInt).map(_.displayValue).getOrElse("")
}
