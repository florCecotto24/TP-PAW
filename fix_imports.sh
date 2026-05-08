#!/bin/bash
TESTDIR="/home/dorbaj/TP-PAW/persistence/src/test/java/ar/edu/itba/paw/persistence/jdbc"

add_import() {
  local file="$1"
  local iface="$2"
  # Only add import if not already present
  if ! grep -q "import ar.edu.itba.paw.persistence.${iface};" "$file"; then
    sed -i "0,/^import/{/^import/i import ar.edu.itba.paw.persistence.${iface};
}" "$file"
    echo "Added import ${iface} to: $(basename "$file")"
  fi
}

add_import "$TESTDIR/CarJdbcDaoTest.java"                   "CarDao"
add_import "$TESTDIR/CarPictureJdbcDaoTest.java"            "CarPictureDao"
add_import "$TESTDIR/EmailVerificationCodeJdbcDaoTest.java" "EmailVerificationCodeDao"
add_import "$TESTDIR/ImageJdbcDaoTest.java"                 "ImageDao"
add_import "$TESTDIR/ListingAvailabilityJdbcDaoTest.java"   "ListingAvailabilityDao"
add_import "$TESTDIR/ListingJdbcDaoTest.java"               "ListingDao"
add_import "$TESTDIR/PasswordResetCodeJdbcDaoTest.java"     "PasswordResetCodeDao"
add_import "$TESTDIR/ReservationJdbcDaoTest.java"           "ReservationDao"
add_import "$TESTDIR/ReviewJdbcDaoTest.java"                "ReviewDao"
add_import "$TESTDIR/StoredFileJdbcDaoTest.java"            "StoredFileDao"
add_import "$TESTDIR/UserJdbcDaoTest.java"                  "UserDao"
