package com.opentext.moj.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "[ContentServerDB].[csuser].[CATREGIONMAP]")
@Data
public class Category {
    @Column(name = "CATID")
    private long catId;

    @Column(name = "CATNAME")
    private String catName;

    @Column(name = "ATTRNAME")
    private String attrName;

    @Id
    @Column(name = "REGIONNAME")
    private String attrId;
}
