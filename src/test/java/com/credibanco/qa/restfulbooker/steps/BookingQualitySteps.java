package com.credibanco.qa.restfulbooker.steps;

import com.credibanco.qa.restfulbooker.config.EnvironmentConfig;
import com.credibanco.qa.restfulbooker.context.ScenarioContext;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;
import com.credibanco.qa.restfulbooker.support.Expect;
import com.credibanco.qa.restfulbooker.support.ResponseSpecs;
import io.cucumber.java.es.Entonces;
import io.restassured.response.Response;

import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.BOOKING_DETAILS_RESPONSE;

/**
 * Quality of the booking information, verified on the stored response of "consulta los detalles de su reserva"
 * (no HTTP here): contract (JSON Schema), response time (SLA) and technical headers.
 */
public class BookingQualitySteps {

    private static final String AGREED_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String SERVER_HEADER = "Server";
    private static final String MEASURED_OPERATION = "GET /booking/{id}";

    private final Actor miguel;
    private final EnvironmentConfig config;

    public BookingQualitySteps(ScenarioContext context) {
        this.miguel = context.miguel();
        this.config = context.config();
    }

    @Entonces("la información recibida incluye todos los datos acordados con su formato correcto")
    public void verifyBookingInformationMatchesContract() {
        bookingDetailsResponse().then().spec(ResponseSpecs.bookingContract());
    }

    @Entonces("la información está disponible dentro del tiempo máximo acordado")
    public void verifyResponseTimeWithinAgreedMaximum() {
        Expect.sla(bookingDetailsResponse(), config.slaBookingGetMs(), MEASURED_OPERATION);
    }

    @Entonces("la información se entrega en el formato y la codificación acordados")
    public void verifyAgreedFormatAndEncoding() {
        bookingDetailsResponse().then().spec(ResponseSpecs.contentType(AGREED_CONTENT_TYPE));
    }

    @Entonces("la respuesta identifica el servidor que la emite")
    public void verifyServerIdentified() {
        Expect.headerPresent(bookingDetailsResponse(), SERVER_HEADER);
    }

    private Response bookingDetailsResponse() {
        return miguel.recall(BOOKING_DETAILS_RESPONSE);
    }
}
