# language: es
@booking
Característica: Creación de reservas de alojamiento
  Como persona que viaja
  quiero registrar una reserva de alojamiento
  para asegurar mi estadía con los datos que indiqué

  @TC-CRE-01 @esc-2 @requested @req
  Escenario: Realizar una reserva de alojamiento
    Dado que Miguel tiene los datos de una nueva reserva de alojamiento
    Cuando registra la reserva
    Entonces la reserva queda confirmada
    Y recibe un número de reserva
    Y la reserva registrada contiene exactamente los datos ingresados
