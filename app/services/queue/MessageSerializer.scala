package services.queue

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, PropertyNamingStrategy}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

trait MessageSerializer {
  def serialize(message: EventMessage): String
}

object JsonMessageSerializer extends MessageSerializer {
  private val mapper = new ObjectMapper()
    .registerModule(DefaultScalaModule)
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
    .configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true)

  def serialize(message: EventMessage): String = toJson(message)

  private def toJson(event: EventMessage) = mapper.writeValueAsString(event)
}