package com.github.cstroe.spendhawk.dvo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntryDvo {
    private Long id;
    private String amount;
    private String transactionDate;
    private String postedDate;
    private String description;
}
