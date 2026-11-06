# language: es
@booking @validation
Característica: Calidad y confiabilidad de la información de reservas
  Como titular de reservas de alojamiento
  quiero recibir la información de mis reservas completa, a tiempo y en el formato acordado
  para poder confiar en ella

  Antecedentes:
    Dado que Miguel tiene una reserva registrada

  @TC-CON-01 @esc-4 @requested @req @contract
  Escenario: Recibir la información completa de una reserva
    Cuando consulta los detalles de su reserva
    Entonces la información recibida incluye todos los datos acordados con su formato correcto

  @TC-CON-02 @esc-4 @requested @req @performance
  Escenario: Consultar una reserva en un tiempo aceptable
    Cuando consulta los detalles de su reserva
    Entonces la información está disponible dentro del tiempo máximo acordado

  @TC-CON-03 @esc-4 @requested @req @contract
  Escenario: Recibir la información en el formato acordado
    Cuando consulta los detalles de su reserva
    Entonces la información se entrega en el formato y la codificación acordados
    Y la respuesta identifica el servidor que la emite
