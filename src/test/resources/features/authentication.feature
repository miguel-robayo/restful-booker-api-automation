# language: es
@auth
Característica: Acceso seguro a la gestión de reservas
  Como titular de reservas de alojamiento
  quiero iniciar sesión y gestionar mis reservas durante esa sesión
  para que solo yo pueda modificarlas o cancelarlas

  @TC-AUTH-01 @esc-1 @requested @req
  Escenario: Iniciar sesión con credenciales válidas
    Dado que Miguel tiene credenciales de acceso válidas
    Cuando inicia sesión
    Entonces el sistema reconoce su identidad
    Y le concede una sesión para gestionar sus reservas

  @TC-AUTH-02 @esc-1 @requested @req
  Escenario: Modificar una reserva después de iniciar sesión
    Dado que Miguel inició sesión
    Y tiene una reserva registrada
    Cuando modifica los datos de su reserva
    Entonces la modificación queda confirmada con los nuevos datos
    Y la reserva conserva los cambios realizados

  @TC-AUTH-02 @esc-1 @requested @req
  Escenario: Cancelar una reserva modificada sin volver a iniciar sesión
    Dado que Miguel inició sesión
    Y tiene una reserva registrada
    Y ya modificó esa reserva durante la misma sesión
    Cuando cancela su reserva
    Entonces la cancelación queda confirmada
    Y la reserva deja de estar disponible
    Y la modificación y la cancelación se realizaron con la misma sesión
    Y no fue necesario iniciar sesión nuevamente

  @TC-NEG-01 @esc-5 @requested @req @negative @security
  Escenario: Intentar modificar una reserva sin haber iniciado sesión
    Dado que Miguel tiene una reserva registrada
    Y conoce el estado actual de su reserva
    Y no ha iniciado sesión
    Cuando intenta modificar su reserva
    Entonces la modificación es rechazada por falta de autorización
    Y la reserva conserva su información original

  @TC-NEG-02 @esc-5 @requested @req @negative @security
  Escenario: Intentar modificar una reserva con una sesión no válida
    Dado que Miguel tiene una reserva registrada
    Y conoce el estado actual de su reserva
    Y utiliza una sesión que el sistema no emitió
    Cuando intenta modificar su reserva
    Entonces la modificación es rechazada por falta de autorización
    Y la reserva conserva su información original
