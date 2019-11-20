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
  lensMake: Option[LensMake],
  lensModel: Option[LensModel],
  lensSerialNumber: Option[LensSerialNumber],
  lensSpecification: Option[LensSpecification],
  componentsConfiguration: Option[ComponentsConfiguration],
  contrast: Option[Contrast],
  brightnessValue: Option[BrightnessValue],
  lightSource: Option[LightSource],
  exposureProgram: Option[ExposureProgram],
  exposureMode: Option[ExposureMode],
  saturation: Option[Saturation],
  sharpness: Option[Sharpness],
  whiteBalance: Option[WhiteBalance],
  digitalZoomRatio: Option[DigitalZoomRatio],
  sceneType: Option[SceneType],
  sceneCaptureType: Option[SceneCaptureType],
  fileSource: Option[FileSource],
  exifVersion: Option[ExifVersion],
  flashpixVersion: Option[FlashpixVersion]
)

object Metadata extends MetadataJsonFields {

  implicit val metadataWrites: Writes[Metadata] = new Writes[Metadata] {
    def writes(v: Metadata): JsObject = toJson(v)

    private def toJson(m: Metadata): JsObject = {
      Json.obj(
        dateTimeFieldOutput                -> Json.toJson(m.dateTime),
        imageDescriptionFieldOutput        -> Json.toJson(m.imageDesc),
        softwareFieldOutput                -> Json.toJson(m.software),
        orientationFieldOutput             -> Json.toJson(m.orientation),
        pixelXDimensionFieldOutput         -> Json.toJson(m.pixelXDimension),
        pixelYDimensionFieldOutput         -> Json.toJson(m.pixelYDimension),
        xResolutionFieldOutput             -> Json.toJson(m.xResolution),
        yResolutionFieldOutput             -> Json.toJson(m.yResolution),
        compressionFieldOutput             -> Json.toJson(m.compression),
        resolutionUnitFieldOutput          -> Json.toJson(m.resolutionUnit),
        colorSpaceFieldOutput              -> Json.toJson(m.colorSpace),
        compressedBitsPerPixelFieldOutput  -> Json.toJson(m.compressedBitsPerPixel),
        makeFieldOutput                    -> Json.toJson(m.make),
        modelFieldOutput                   -> Json.toJson(m.model),
        bodySerialNumberFieldOutput        -> Json.toJson(m.bodySerialNumber),
        fNumberFieldOutput                 -> Json.toJson(m.fNumber),
        exposureTimeFieldOutput            -> Json.toJson(m.exposureTime),
        shutterSpeedValueFieldOutput       -> Json.toJson(m.shutterSpeedValue),
        isoSpeedRatingsFieldOutput         -> Json.toJson(m.isoSpeedRatings),
        exposureBiasValueFieldOutput       -> Json.toJson(m.exposureBiasValue),
        focalLengthFieldOutput             -> Json.toJson(m.focalLength),
        focalLengthIn35mmFilmFieldOutput   -> Json.toJson(m.focalLengthIn35mmFilm),
        apertureValueFieldOutput           -> Json.toJson(m.apertureValue),
        maxApertureValueFieldOutput        -> Json.toJson(m.maxApertureValue),
        meteringModeFieldOutput            -> Json.toJson(m.meteringMode),
        subjectDistanceFieldOutput         -> Json.toJson(m.subjectDistance),
        flashFieldOutput                   -> Json.toJson(m.flash),
        dateTimeOriginalFieldOutput        -> Json.toJson(m.dateTimeOriginal),
        dateTimeDigitizedFieldOutput       -> Json.toJson(m.dateTimeDigitized),
        lensMakeFieldOutput                -> Json.toJson(m.lensMake),
        lensModelFieldOutput               -> Json.toJson(m.lensModel),
        lensSerialNumberFieldOutput        -> Json.toJson(m.lensSerialNumber),
        lensSpecificationFieldOutput       -> Json.toJson(m.lensSpecification),
        componentsConfigurationFieldOutput -> Json.toJson(m.componentsConfiguration),
        contrastFieldOutput                -> Json.toJson(m.contrast),
        brightnessValueFieldOutput         -> Json.toJson(m.brightnessValue),
        lightSourceFieldOutput             -> Json.toJson(m.lightSource),
        exposureProgramFieldOutput         -> Json.toJson(m.exposureProgram),
        exposureModeFieldOutput            -> Json.toJson(m.exposureMode),
        saturationFieldOutput              -> Json.toJson(m.saturation),
        sharpnessFieldOutput               -> Json.toJson(m.sharpness),
        whiteBalanceFieldOutput            -> Json.toJson(m.whiteBalance),
        digitalZoomRatioFieldOutput        -> Json.toJson(m.digitalZoomRatio),
        sceneTypeFieldOutput               -> Json.toJson(m.sceneType),
        sceneCaptureTypeFieldOutput        -> Json.toJson(m.sceneCaptureType),
        fileSourceFieldOutput              -> Json.toJson(m.fileSource),
        exifVersionFieldOutput             -> Json.toJson(m.exifVersion),
        flashpixVersionFieldOutput         -> Json.toJson(m.flashpixVersion)
      )
    }
  }
}