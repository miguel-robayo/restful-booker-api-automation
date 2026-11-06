# language: es
@booking @query
Característica: Consulta y búsqueda de reservas
  Como titular de reservas de alojamiento
  quiero encontrar mis reservas y consultar su detalle
  para conocer cuáles superan un valor de referencia y saber cuándo una reserva no existe

  @TC-QRY-01 @esc-3 @requested @req
  Escenario: Identificar sus reservas de valor superior al de referencia
    Dado que Miguel tiene cuatro reservas a su nombre con valores alrededor del valor de referencia
    Cuando busca entre sus reservas las de valor superior al de referencia
    Entonces la búsqueda por su nombre encuentra exactamente sus cuatro reservas
    Y solo se identifican las reservas que superan el valor de referencia
    Y el detalle de la reserva identificada coincide con el registrado

  @TC-NEG-03 @esc-5 @requested @req @negative
  Escenario: Consultar una reserva inexistente
    Dado que Miguel tiene un número de reserva que no corresponde a ninguna reserva existente
    Cuando consulta esa reserva
    Entonces el sistema informa que la reserva no existe
