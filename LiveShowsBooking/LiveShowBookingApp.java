import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LiveShowBookingApp {

    /**
     * Genre enumeration for different types of shows.
     */
    public enum Genre {
        COMEDY,
        THEATER,
        TECH,
        SINGING,
        DANCE
        // Add more genres if needed
    }

    /**
     * Represents a Show with a unique name, genre, and a specific timeslot.
     * For simplicity, we assume a single-hour slot: startTime to startTime+1hr.
     */
    public static class Show {
        private final String showName;   // Unique identifier
        private final Genre genre;
        private final LocalTime startTime;
        private final LocalTime endTime;

        public Show(String showName, Genre genre, LocalTime startTime, LocalTime endTime) {
            this.showName = showName;
            this.genre = genre;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public String getShowName() {
            return showName;
        }

        public Genre getGenre() {
            return genre;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        @Override
        public String toString() {
            return "Show{" +
                    "name='" + showName + '\'' +
                    ", genre=" + genre +
                    ", time=" + startTime + "-" + endTime +
                    '}';
        }
    }

    /**
     * Represents a booking made by a user for a particular show.
     */
    public static class Booking {
        private final String bookingId;
        private final String userName; // user identifier
        private final Show show;
        private final int numPersons;

        public Booking(String bookingId, String userName, Show show, int numPersons) {
            this.bookingId = bookingId;
            this.userName = userName;
            this.show = show;
            this.numPersons = numPersons;
        }

        public String getBookingId() {
            return bookingId;
        }

        public String getUserName() {
            return userName;
        }

        public Show getShow() {
            return show;
        }

        public int getNumPersons() {
            return numPersons;
        }

        @Override
        public String toString() {
            return "Booking{" +
                    "bookingId='" + bookingId + '\'' +
                    ", userName='" + userName + '\'' +
                    ", showName='" + show.getShowName() + '\'' +
                    ", numPersons=" + numPersons +
                    ", timeslot=" + show.getStartTime() + "-" + show.getEndTime() +
                    '}';
        }
    }

    /**
     * A simple service to manage Shows.
     */
    public static class ShowService {
        // For simplicity, we limit total shows in a day to 9 as per requirement.
        private static final int MAX_SHOWS_PER_DAY = 9;

        // Key: showName, Value: Show object
        private final Map<String, Show> showsMap = new ConcurrentHashMap<>();

        // Lock for concurrency around show registration
        private final ReentrantLock lock = new ReentrantLock();

        /**
         * Registers a new show if the limit (9) is not reached.
         * Assumes timeslots are 1-hour each from 9 AM to 9 PM.
         */
        public Show registerShow(String showName, Genre genre, LocalTime startTime) {
            lock.lock();
            try {
                if (showsMap.size() >= MAX_SHOWS_PER_DAY) {
                    throw new IllegalStateException("Cannot register more shows. Limit reached (9).");
                }
                if (showsMap.containsKey(showName)) {
                    throw new IllegalArgumentException("Show with this name already exists.");
                }
                // End time is startTime plus 1 hour (for simplicity).
                LocalTime endTime = startTime.plusHours(1);
                Show newShow = new Show(showName, genre, startTime, endTime);
                showsMap.put(showName, newShow);
                return newShow;
            } finally {
                lock.unlock();
            }
        }

        /**
         * Returns the show by name if it exists, otherwise null.
         */
        public Show getShowByName(String showName) {
            return showsMap.get(showName);
        }

        /**
         * Lists all shows in random order (per requirement).
         */
        public List<Show> listAllShowsRandomOrder() {
            List<Show> allShows = new ArrayList<>(showsMap.values());
            Collections.shuffle(allShows);  // randomize order
            return allShows;
        }
    }

    /**
     * Service to manage bookings. We store bookings keyed by user and also
     * maintain an index by show for easy lookups if needed.
     */
    public static class BookingService {
        // Key: userName, Value: list of bookings for that user
        private final Map<String, List<Booking>> userBookingsMap = new ConcurrentHashMap<>();
        // Also store all bookings in a global list (optional, helps in certain queries)
        private final List<Booking> allBookings = Collections.synchronizedList(new ArrayList<>());

        // Lock for concurrency around booking logic
        private final ReentrantLock lock = new ReentrantLock();

        /**
         * Books a show for a user (up to 3 persons in one request as stated).
         * Enforces rule: user cannot book two different shows in the same time slot.
         */
        public Booking bookShow(String userName, Show show, int numPersons) {
            if (numPersons < 1 || numPersons > 3) {
                throw new IllegalArgumentException("You can only book between 1 to 3 persons in a single request.");
            }

            lock.lock();
            try {
                // Check if user already has a booking in the same timeslot
                if (hasBookingInTimeSlot(userName, show.getStartTime(), show.getEndTime())) {
                    throw new IllegalStateException("User already booked a show in this timeslot.");
                }

                // Create a new booking
                String bookingId = UUID.randomUUID().toString();
                Booking newBooking = new Booking(bookingId, userName, show, numPersons);

                // Save booking
                userBookingsMap.putIfAbsent(userName, new ArrayList<>());
                userBookingsMap.get(userName).add(newBooking);
                allBookings.add(newBooking);

                return newBooking;
            } finally {
                lock.unlock();
            }
        }

        /**
         * Cancels a user's booking for a particular show.
         */
        public boolean cancelBooking(String userName, String showName) {
            lock.lock();
            try {
                List<Booking> bookings = userBookingsMap.get(userName);
                if (bookings == null || bookings.isEmpty()) {
                    return false;  // no bookings found for this user
                }
                Iterator<Booking> iterator = bookings.iterator();
                boolean cancelled = false;
                while (iterator.hasNext()) {
                    Booking b = iterator.next();
                    if (b.getShow().getShowName().equals(showName)) {
                        iterator.remove();
                        allBookings.remove(b);
                        cancelled = true;
                    }
                }
                return cancelled;
            } finally {
                lock.unlock();
            }
        }

        /**
         * Checks if a user has any booking that overlaps with the given timeslot.
         */
        private boolean hasBookingInTimeSlot(String userName, LocalTime start, LocalTime end) {
            List<Booking> userBookings = userBookingsMap.getOrDefault(userName, Collections.emptyList());
            for (Booking b : userBookings) {
                // If time intervals overlap, return true
                if (timeSlotsOverlap(b.getShow().getStartTime(), b.getShow().getEndTime(), start, end)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Helper to check timeslot overlap for 1-hour blocks. 
         * (Since everything is discrete, this is simplified.)
         */
        private boolean timeSlotsOverlap(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
            // Overlap if the start is before the other end, and the other start is before this end
            return !s1.isAfter(e2) && !s2.isAfter(e1);
        }

        /**
         * List all bookings for a particular user.
         */
        public List<Booking> listUserBookings(String userName) {
            return userBookingsMap.getOrDefault(userName, Collections.emptyList());
        }

        /**
         * List all bookings (global).
         */
        public List<Booking> listAllBookings() {
            return new ArrayList<>(allBookings);
        }
    }

    /**
     * Demonstration: main() method that ties everything together.
     */
    public static void main(String[] args) {
        ShowService showService = new ShowService();
        BookingService bookingService = new BookingService();

        // 1) Register up to 9 new shows
        // For simplicity, let's create a few shows in the 9am-9pm window
        showService.registerShow("MorningComedy", Genre.COMEDY, LocalTime.of(9, 0));
        showService.registerShow("TechTalk1", Genre.TECH, LocalTime.of(10, 0));
        showService.registerShow("SingingStar", Genre.SINGING, LocalTime.of(11, 0));
        showService.registerShow("AfternoonPlay", Genre.THEATER, LocalTime.of(14, 0));
        showService.registerShow("DanceHour", Genre.DANCE, LocalTime.of(15, 0));
        // ... we can go up to 9 total

        // 2) List shows in random order
        System.out.println("All shows in random order:");
        List<Show> randomShows = showService.listAllShowsRandomOrder();
        for (Show show : randomShows) {
            System.out.println(show);
        }
        System.out.println("-------------------------------------------------------");

        // 3) Book tickets for a show
        // Assume userName is "Alice"
        try {
            Booking booking1 = bookingService.bookShow("Alice", showService.getShowByName("MorningComedy"), 2);
            System.out.println("Booking successful: " + booking1);
        } catch (Exception e) {
            System.out.println("Booking failed: " + e.getMessage());
        }

        // 4) Attempt to book the same user in the same time slot for a different show
        // This should fail due to timeslot overlap
        try {
            Booking booking2 = bookingService.bookShow("Alice", showService.getShowByName("TechTalk1"), 3);
            System.out.println("Booking successful: " + booking2);
        } catch (Exception e) {
            System.out.println("Booking failed: " + e.getMessage());
        }
        System.out.println("-------------------------------------------------------");

        // 5) Cancel an existing booking
        boolean cancelStatus = bookingService.cancelBooking("Alice", "MorningComedy");
        System.out.println("Cancel booking status for 'MorningComedy': " + cancelStatus);

        // 6) Re-try booking for "TechTalk1" after cancellation
        try {
            Booking booking2 = bookingService.bookShow("Alice", showService.getShowByName("TechTalk1"), 3);
            System.out.println("Booking successful: " + booking2);
        } catch (Exception e) {
            System.out.println("Booking failed: " + e.getMessage());
        }
        System.out.println("-------------------------------------------------------");

        // 7) View all bookings for a user
        List<Booking> aliceBookings = bookingService.listUserBookings("Alice");
        System.out.println("Alice's bookings:");
        for (Booking b : aliceBookings) {
            System.out.println(b);
        }
    }
}
