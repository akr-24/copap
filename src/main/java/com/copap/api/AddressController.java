package com.copap.api;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/addresses")
public class AddressController {

    private final JdbcTemplate jdbcTemplate;

    public AddressController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAddresses(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        var addresses = jdbcTemplate.query(
                "SELECT address_id, label, full_name, phone, street, city, state, postal_code, country, is_default " +
                "FROM addresses WHERE user_id = ? ORDER BY is_default DESC, created_at DESC",
                (rs, rowNum) -> Map.<String, Object>of(
                        "addressId",  rs.getString("address_id"),
                        "label",      rs.getString("label") != null ? rs.getString("label") : "",
                        "fullName",   rs.getString("full_name"),
                        "phone",      rs.getString("phone") != null ? rs.getString("phone") : "",
                        "street",     rs.getString("street"),
                        "city",       rs.getString("city"),
                        "state",      rs.getString("state") != null ? rs.getString("state") : "",
                        "postalCode", rs.getString("postal_code"),
                        "country",    rs.getString("country") != null ? rs.getString("country") : "India",
                        "isDefault",  rs.getBoolean("is_default")
                ),
                userId
        );
        return ResponseEntity.ok(addresses);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createAddress(
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        String userId = (String) auth.getPrincipal();
        String addressId = UUID.randomUUID().toString();
        String label      = (String) body.getOrDefault("label", "Home");
        String fullName   = (String) body.get("fullName");
        String phone      = (String) body.getOrDefault("phone", "");
        String street     = (String) body.get("street");
        String city       = (String) body.get("city");
        String state      = (String) body.getOrDefault("state", "");
        String postalCode = (String) body.get("postalCode");
        String country    = (String) body.getOrDefault("country", "India");
        boolean isDefault = Boolean.TRUE.equals(body.get("isDefault"));

        if (isDefault) {
            jdbcTemplate.update("UPDATE addresses SET is_default = false WHERE user_id = ?", userId);
        }

        jdbcTemplate.update(
                "INSERT INTO addresses (address_id, user_id, label, full_name, phone, street, city, state, postal_code, country, is_default, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                addressId, userId, label, fullName, phone, street, city, state, postalCode, country, isDefault
        );

        return ResponseEntity.status(201).body(Map.of(
                "addressId",  addressId,
                "label",      label,
                "fullName",   fullName != null ? fullName : "",
                "phone",      phone,
                "street",     street != null ? street : "",
                "city",       city != null ? city : "",
                "state",      state,
                "postalCode", postalCode != null ? postalCode : "",
                "country",    country,
                "isDefault",  isDefault
        ));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<Map<String, Boolean>> updateAddress(
            @PathVariable String addressId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        String userId = (String) auth.getPrincipal();

        if (body.containsKey("label")) {
            jdbcTemplate.update("UPDATE addresses SET label = ? WHERE address_id = ? AND user_id = ?",
                    body.get("label"), addressId, userId);
        }
        if (body.containsKey("fullName")) {
            jdbcTemplate.update("UPDATE addresses SET full_name = ? WHERE address_id = ? AND user_id = ?",
                    body.get("fullName"), addressId, userId);
        }
        if (body.containsKey("phone")) {
            jdbcTemplate.update("UPDATE addresses SET phone = ? WHERE address_id = ? AND user_id = ?",
                    body.get("phone"), addressId, userId);
        }
        if (body.containsKey("street")) {
            jdbcTemplate.update("UPDATE addresses SET street = ? WHERE address_id = ? AND user_id = ?",
                    body.get("street"), addressId, userId);
        }
        if (body.containsKey("city")) {
            jdbcTemplate.update("UPDATE addresses SET city = ? WHERE address_id = ? AND user_id = ?",
                    body.get("city"), addressId, userId);
        }
        if (body.containsKey("state")) {
            jdbcTemplate.update("UPDATE addresses SET state = ? WHERE address_id = ? AND user_id = ?",
                    body.get("state"), addressId, userId);
        }
        if (body.containsKey("postalCode")) {
            jdbcTemplate.update("UPDATE addresses SET postal_code = ? WHERE address_id = ? AND user_id = ?",
                    body.get("postalCode"), addressId, userId);
        }
        if (body.containsKey("country")) {
            jdbcTemplate.update("UPDATE addresses SET country = ? WHERE address_id = ? AND user_id = ?",
                    body.get("country"), addressId, userId);
        }

        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Map<String, Boolean>> deleteAddress(
            @PathVariable String addressId,
            Authentication auth) {

        String userId = (String) auth.getPrincipal();
        jdbcTemplate.update(
                "DELETE FROM addresses WHERE address_id = ? AND user_id = ?",
                addressId, userId
        );
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{addressId}/default")
    public ResponseEntity<Map<String, Boolean>> setDefaultAddress(
            @PathVariable String addressId,
            Authentication auth) {

        String userId = (String) auth.getPrincipal();
        jdbcTemplate.update("UPDATE addresses SET is_default = false WHERE user_id = ?", userId);
        jdbcTemplate.update(
                "UPDATE addresses SET is_default = true WHERE address_id = ? AND user_id = ?",
                addressId, userId
        );
        return ResponseEntity.ok(Map.of("success", true));
    }
}
