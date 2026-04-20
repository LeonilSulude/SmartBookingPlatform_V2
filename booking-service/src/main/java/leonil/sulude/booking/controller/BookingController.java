
package leonil.sulude.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import leonil.sulude.booking.dto.BookingRequestDTO;
import leonil.sulude.booking.dto.BookingResponseDTO;
import leonil.sulude.booking.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Tag(
        name = "Booking",
        description = "Endpoints for managing service bookings"
)
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    /**
     * Returns all bookings stored in the system.
     *
     * @return List of bookings
     */
    @Operation(
            summary = "Retrieve all bookings",
            description = "Returns a list of all bookings stored in the system."
    )
    @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully")
    @GetMapping
    public List<BookingResponseDTO> getAll() {
        return service.getAll();
    }

    /**
     * Returns a specific booking by its ID.
     *
     * @param id Booking ID
     * @return Booking if found, 404 otherwise
     */
    @Operation(
            summary = "Retrieve a booking by ID",
            description = "Returns details of a specific booking."
    )
    @ApiResponse(responseCode = "200", description = "Booking found")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDTO> getById(@PathVariable UUID id) {
        Optional<BookingResponseDTO> booking = service.getById(id);
        return booking.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new booking for a service resource.
     *
     * @param booking Request body containing booking details
     * @return The created booking with location header
     */
    @Operation(
            summary = "Create a new booking",
            description = "Creates a booking for a specific service resource within a selected time period."
    )
    @ApiResponse(responseCode = "201", description = "Booking created successfully")
    @ApiResponse(responseCode = "409", description = "Booking conflict or resource unavailable")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @PostMapping
    public ResponseEntity<BookingResponseDTO> create(@Valid @RequestBody BookingRequestDTO booking) {

        BookingResponseDTO created = service.create(booking);
        URI location = URI.create("/api/bookings/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Deletes a booking by its ID.
     *
     * @param id Booking ID
     * @return 204 if deleted, 404 otherwise
     */
    @Operation(
            summary = "Delete a booking",
            description = "Deletes a booking by its unique identifier."
    )
    @ApiResponse(responseCode = "204", description = "Booking deleted successfully")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}