package com.project.planner.trip;

import com.project.planner.participant.ParticipantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;


@RestController
@RequestMapping("/trips")
public class TripController {

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private TripRepository repository;

    @PostMapping
    public ResponseEntity<TripCreateResponse> createTrip(@RequestBody TripRequestPayload payload){

        Trip newTrip = new Trip(payload);

        this.repository.save(newTrip);

        this.participantService.registerParticipantsToEvent(payload.emails_to_invite(), newTrip);

        return ResponseEntity.ok(new TripCreateResponse(newTrip.getId()));

    }

    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTripDetails(@PathVariable UUID id){

        Optional<Trip> trip = repository.findById(id); // resultado da busca do banco de dados

        return trip.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build()); //caso encontre uma trip status ok, se for nulo
                                                                                                // não encontrou um response entity para aquele id
                                                                                                //vai retornar um status notFound, não encontrado.
    }
    @PutMapping("/{id}")
    public ResponseEntity<Trip> updateTrip(@PathVariable UUID id, @RequestBody TripRequestPayload payload) {

        Optional<Trip> trip = repository.findById(id); // resultado da busca do banco de dados

        if (trip.isPresent()) {
            Trip rawTip = trip.get();
            rawTip.setEndsAt(LocalDateTime.parse(payload.ends_at(), DateTimeFormatter.ISO_DATE_TIME));
            rawTip.setStartsAt(LocalDateTime.parse(payload.starts_at(), DateTimeFormatter.ISO_DATE_TIME));
            rawTip.setDestination(payload.destination());

            this.repository.save(rawTip);

            return ResponseEntity.ok(rawTip);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/confirm")
    public ResponseEntity<Trip> confirmTrip(@PathVariable UUID id) {

        Optional<Trip> trip = repository.findById(id); // resultado da busca do banco de dados, procura por uma viagem com esse id
        //optional é uma opção, talvez a viagem esteja lá, mas pode não estar

        if (trip.isPresent()) { // caso a viagem esteja presente
            Trip rawTip = trip.get(); // extrai o objeto opcional de dentro do optional
           rawTip.setIsConfirmed(true); //muda o status de false para true, pois a viagem está confirmada

            this.repository.save(rawTip); // salva no banco de dados
            this.participantService.triggerConfirmationEmailToParticipants(id); // e-mail de confirmação

            return ResponseEntity.ok(rawTip); // retorna um status 200 ok
        }

        return ResponseEntity.notFound().build(); // se não manda um notFound
    }
}
