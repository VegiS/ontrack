-- 5. Shared build filters

CREATE TABLE SHARED_BUILD_FILTERS (
  BRANCHID INTEGER      NOT NULL,
  NAME     VARCHAR(120) NOT NULL,
  TYPE     VARCHAR(150) NOT NULL,
  DATA     TEXT         NOT NULL,
  CONSTRAINT SHARED_BUILD_FILTERS_PK PRIMARY KEY (BRANCHID, NAME),
  CONSTRAINT SHARED_BUILD_FILTERS_FK_BRANCH FOREIGN KEY (BRANCHID) REFERENCES BRANCHES (ID)
    ON DELETE CASCADE
);
