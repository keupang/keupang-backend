package com.example.keupanguser.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public record LoginRequest(String userEmail, String userPassword){
}
