package services.rti.metadata

import play.api.libs.json._

case class Metadata(
  // Image Information
  dateTime: Option[DateTime],
  imageDesc: Option[ImageDescription],
  software: Option[Software],
  orientation: Option[Orientation],
  pixelXDimension: Option[PixelXDimension],
  pixelYDimension: Option[PixelYDimension],
  xResolution: Option[XResolution],
  yResolution: Option[YResolution],
  compression: Option[Compression],
  resolutionUnit: Option[ResolutionUnit],
  colorSpace: Option[ColorSpace],
  compressedBitsPerPixel: Option[CompressedBitsPerPixel],
  // Camera Information
  make: Option[Make],
  model: Option[Model],
  bodySerialNumber: Option[BodySerialNumber],
  fNumber: Option[FNumber],
  exposureTime: Option[ExposureTime],
  shutterSpeedValue: Option[ShutterSpeedValue],
  isoSpeedRatings: Option[ISOSpeedRatings],
  exposureBiasValue: Option[ExposureBiasValue],
  focalLength: Option[FocalLength],
  focalLengthIn35mmFilm: Option[FocalLengthIn35mmFilm],
  apertureValue: Option[ApertureValue],
  maxApertureValue: Option[MaxApertureValue],
  meteringMode: Option[MeteringMode],
  subjectDistance: Option[SubjectDistance],
  flash: Option[Flash],
  // Advanced Information
  dateTimeOriginal: Option[DateTimeOriginal],
  dateTimeDigitized: Option[DateTimeDigitized],
)

object Metadata extends MetadataJsonFields {

  implicit val metadataWrites: Writes[Metadata] = new Writes[Metadata] {
    def writes(v: Metadata): JsObject = toJson(v)

    private def toJson(m: Metadata): JsObject = {
      Json.obj(
        dateTimeFieldCamelCase               -> Json.toJson(m.dateTime),
        imageDescriptionFieldCamelCase       -> Json.toJson(m.imageDesc),
        softwareFieldCamelCase               -> Json.toJson(m.software),
        orientationFieldCamelCase            -> Json.toJson(m.orientation),
        pixelXDimensionFieldCamelCase        -> Json.toJson(m.pixelXDimension),
        pixelYDimensionFieldCamelCase        -> Json.toJson(m.pixelYDimension),
        xResolutionFieldCamelCase            -> Json.toJson(m.xResolution),
        yResolutionFieldCamelCase            -> Json.toJson(m.yResolution),
        compressionFieldCamelCase            -> Json.toJson(m.compression),
        resolutionUnitFieldCamelCase         -> Json.toJson(m.resolutionUnit),
        colorSpaceFieldCamelCase             -> Json.toJson(m.colorSpace),
        compressedBitsPerPixelFieldCamelCase -> Json.toJson(m.compressedBitsPerPixel),
        makeFieldCamelCase                   -> Json.toJson(m.make),
        modelFieldCamelCase                  -> Json.toJson(m.model),
        bodySerialNumberFieldCamelCase       -> Json.toJson(m.bodySerialNumber),
        fNumberFieldCamelCase                -> Json.toJson(m.fNumber),
        exposureTimeFieldCamelCase           -> Json.toJson(m.exposureTime),
        shutterSpeedValueFieldCamelCase      -> Json.toJson(m.shutterSpeedValue),
        isoSpeedRatingsFieldCamelCase        -> Json.toJson(m.isoSpeedRatings),
        exposureBiasValueFieldCamelCase      -> Json.toJson(m.exposureBiasValue),
        focalLengthFieldCamelCase            -> Json.toJson(m.focalLength),
        focalLengthIn35mmFilmFieldCamelCase  -> Json.toJson(m.focalLengthIn35mmFilm),
        apertureValueFieldCamelCase          -> Json.toJson(m.apertureValue),
        maxApertureValueFieldCamelCase       -> Json.toJson(m.maxApertureValue),
        meteringModeFieldCamelCase           -> Json.toJson(m.meteringMode),
        subjectDistanceFieldCamelCase        -> Json.toJson(m.subjectDistance),
        flashFieldCamelCase                  -> Json.toJson(m.flash),
        dateTimeOriginalFieldCamelCase       -> Json.toJson(m.dateTimeOriginal),
        dateTimeDigitizedFieldCamelCase      -> Json.toJson(m.dateTimeDigitized),
      )
    }
  }
}
