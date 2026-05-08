#!/bin/bash
TESTDIR="/home/dorbaj/TP-PAW/persistence/src/test/java/ar/edu/itba/paw/persistence/jdbc"

update_test() {
  local file="$1"
  local old_class="$2"
  local new_iface="$3"
  sed -i "s|import ar.edu.itba.paw.persistence.jdbc.${old_class};|import ar.edu.itba.paw.persistence.${new_iface};|g" "$file"
  sed -i "s|private ${old_class} |private ${new_iface} |g" "$file"
  echo "Updated: $(basename "$file")"
}

update_test "$TESTDIR/CarJdbcDaoTest.java"                   "CarJdbcDao"                   "CarDao"
update_test "$TESTDIR/CarPictureJdbcDaoTest.java"            "CarPictureJdbcDao"             "CarPictureDao"
update_test "$TESTDIR/EmailVerificationCodeJdbcDaoTest.java" "EmailVerificationCodeJdbcDao"  "EmailVerificationCodeDao"
update_test "$TESTDIR/ImageJdbcDaoTest.java"                 "ImageJdbcDao"                  "ImageDao"
update_test "$TESTDIR/ListingAvailabilityJdbcDaoTest.java"   "ListingAvailabilityJdbcDao"    "ListingAvailabilityDao"
update_test "$TESTDIR/ListingJdbcDaoTest.java"               "ListingJdbcDao"                "ListingDao"
update_test "$TESTDIR/PasswordResetCodeJdbcDaoTest.java"     "PasswordResetCodeJdbcDao"      "PasswordResetCodeDao"
update_test "$TESTDIR/ReservationJdbcDaoTest.java"           "ReservationJdbcDao"            "ReservationDao"
update_test "$TESTDIR/ReviewJdbcDaoTest.java"                "ReviewJdbcDao"                 "ReviewDao"
update_test "$TESTDIR/StoredFileJdbcDaoTest.java"            "StoredFileJdbcDao"             "StoredFileDao"
update_test "$TESTDIR/UserJdbcDaoTest.java"                  "UserJdbcDao"                   "UserDao"
