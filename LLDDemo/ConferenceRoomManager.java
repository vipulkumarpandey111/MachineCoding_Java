import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

// ---------- Custom Exceptions ----------
class InvalidBookingException extends Exception {
    public InvalidBookingException(String message) {
        super(message);
    }
}

class RoomNotAvailableException extends Exception {
    public RoomNotAvailableException(String message) {
        super(message);
    }
}

// ---------- Factory Pattern for Object Creation ----------
class ConferenceFactory {
    public static ConferenceRoom createConferenceRoom(String name, int floorNumber, String buildingName) {
        return new ConferenceRoom(name, floorNumber, buildingName);
    }
}

// ---------- Conference Room (Entity) ----------
class ConferenceRoom {
    String name;
    int floorNumber;
    String buildingName;
    // Each booking is represented as an int[] with two elements: start and end.
    List<int[]> bookings;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public ConferenceRoom(String name, int floorNumber, String buildingName) {
        this.name = name;
        this.floorNumber = floorNumber;
        this.buildingName = buildingName;
        this.bookings = new ArrayList<>();
    }

    // Checks if the room is available for the given slot
    public boolean isAvailable(int start, int end) {
        lock.readLock().lock();
        try {
            for (int[] slot : bookings) {
                // if overlap then not available
                if (!(end <= slot[0] || start >= slot[1])) {
                    return false;
                }
            }
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }

    // Books the room if available
    public void bookRoom(int start, int end) throws RoomNotAvailableException, InvalidBookingException {
        if (end - start > 12) {
            throw new InvalidBookingException("Cannot book for more than 12 hours.");
        }
        lock.writeLock().lock();
        try {
            if (!isAvailable(start, end)) {
                throw new RoomNotAvailableException("Room " + name + " is not available for the slot " + start + ":" + end);
            }
            // Add the booking
            bookings.add(new int[]{start, end});
            // Sort bookings by start time for easier management
            Collections.sort(bookings, new Comparator<int[]>() {
                public int compare(int[] a, int[] b) {
                    return Integer.compare(a[0], b[0]);
                }
            });
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Cancels the booking if it exists
    public void cancelBooking(int start, int end) {
        lock.writeLock().lock();
        try {
            Iterator<int[]> iterator = bookings.iterator();
            while (iterator.hasNext()) {
                int[] slot = iterator.next();
                if (slot[0] == start && slot[1] == end) {
                    iterator.remove();
                    break;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Returns the list of bookings for this room
    public List<int[]> getBookings() {
        lock.readLock().lock();
        try {
            // Return a copy for thread-safety.
            return new ArrayList<>(bookings);
        } finally {
            lock.readLock().unlock();
        }
    }

    // Computes available slots for the day (working hours 1 to 24) based on current bookings
    public List<int[]> getAvailableSlots() {
        lock.readLock().lock();
        try {
            List<int[]> freeSlots = new ArrayList<>();
            int dayStart = 1;
            int dayEnd = 24;
            // Sort bookings first (should already be sorted)
            Collections.sort(bookings, new Comparator<int[]>() {
                public int compare(int[] a, int[] b) {
                    return Integer.compare(a[0], b[0]);
                }
            });
            // Find gap before the first booking
            if (bookings.isEmpty()) {
                freeSlots.add(new int[]{dayStart, dayEnd});
            } else {
                if (dayStart < bookings.get(0)[0]) {
                    freeSlots.add(new int[]{dayStart, bookings.get(0)[0]});
                }
                // Find gaps between bookings
                for (int i = 0; i < bookings.size() - 1; i++) {
                    int endCurrent = bookings.get(i)[1];
                    int startNext = bookings.get(i + 1)[0];
                    if (endCurrent < startNext) {
                        freeSlots.add(new int[]{endCurrent, startNext});
                    }
                }
                // Gap after the last booking
                int lastEnd = bookings.get(bookings.size() - 1)[1];
                if (lastEnd < dayEnd) {
                    freeSlots.add(new int[]{lastEnd, dayEnd});
                }
            }
            return freeSlots;
        } finally {
            lock.readLock().unlock();
        }
    }
}

// ---------- Floor (Entity) ----------
class Floor {
    int floorNumber;
    List<ConferenceRoom> rooms;

    public Floor(int floorNumber) {
        this.floorNumber = floorNumber;
        this.rooms = new ArrayList<>();
    }

    public void addConferenceRoom(String roomName, String buildingName) {
        ConferenceRoom room = ConferenceFactory.createConferenceRoom(roomName, floorNumber, buildingName);
        rooms.add(room);
    }
}

// ---------- Building (Entity) ----------
class Building {
    String name;
    Map<Integer, Floor> floors;

    public Building(String name) {
        this.name = name;
        floors = new HashMap<>();
    }

    public void addFloor(int floorNumber) {
        if (!floors.containsKey(floorNumber)) {
            floors.put(floorNumber, new Floor(floorNumber));
        }
    }

    public void addConferenceRoom(String roomName, int floorNumber) {
        if (floors.containsKey(floorNumber)) {
            floors.get(floorNumber).addConferenceRoom(roomName, name);
        }
    }
}

// ---------- Service Layer (Singleton + Concurrency) ----------
class ConferenceRoomService {
    private static ConferenceRoomService instance;
    private final Map<String, Building> buildings;
    private final ReentrantLock serviceLock = new ReentrantLock();

    private ConferenceRoomService() {
        buildings = new ConcurrentHashMap<>();
    }

    public static ConferenceRoomService getInstance() {
        if (instance == null) {
            synchronized (ConferenceRoomService.class) {
                if (instance == null) {
                    instance = new ConferenceRoomService();
                }
            }
        }
        return instance;
    }

    public void addBuilding(String name) {
        serviceLock.lock();
        try {
            buildings.put(name, new Building(name));
            System.out.println("Added building " + name + " into the system.");
        } finally {
            serviceLock.unlock();
        }
    }

    public void addFloor(String buildingName, int floorNumber) {
        serviceLock.lock();
        try {
            if (buildings.containsKey(buildingName)) {
                buildings.get(buildingName).addFloor(floorNumber);
                System.out.println("Added floor " + floorNumber + " in building " + buildingName + ".");
            } else {
                System.out.println("Building " + buildingName + " does not exist.");
            }
        } finally {
            serviceLock.unlock();
        }
    }

    public void addConferenceRoom(String buildingName, int floorNumber, String roomName) {
        serviceLock.lock();
        try {
            if (buildings.containsKey(buildingName)) {
                buildings.get(buildingName).addConferenceRoom(roomName, floorNumber);
                System.out.println("Added conference room " + roomName + " in floor " + floorNumber + " of building " + buildingName + ".");
            } else {
                System.out.println("Building " + buildingName + " does not exist.");
            }
        } finally {
            serviceLock.unlock();
        }
    }

    // Lists all conference rooms with their available slots
    public void listRooms() {
        serviceLock.lock();
        try {
            for (Building building : buildings.values()) {
                for (Floor floor : building.floors.values()) {
                    for (ConferenceRoom room : floor.rooms) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(room.name).append(" ").append(room.floorNumber).append(" ").append(building.name).append(" ");
                        List<int[]> freeSlots = room.getAvailableSlots();
                        sb.append("[");
                        for (int i = 0; i < freeSlots.size(); i++) {
                            int[] slot = freeSlots.get(i);
                            sb.append("{").append(slot[0]).append(":").append(slot[1]).append("}");
                            if (i < freeSlots.size() - 1) {
                                sb.append(", ");
                            }
                        }
                        sb.append("]");
                        System.out.println(sb.toString());
                    }
                }
            }
        } finally {
            serviceLock.unlock();
        }
    }

    // Books a room given the slot, building, floor, and room name.
    public void bookRoom(String buildingName, int floorNumber, String roomName, int start, int end) {
        serviceLock.lock();
        try {
            Building building = buildings.get(buildingName);
            if (building == null) {
                System.out.println("Building " + buildingName + " does not exist.");
                return;
            }
            Floor floor = building.floors.get(floorNumber);
            if (floor == null) {
                System.out.println("Floor " + floorNumber + " does not exist in building " + buildingName + ".");
                return;
            }
            ConferenceRoom room = null;
            for (ConferenceRoom r : floor.rooms) {
                if (r.name.equals(roomName)) {
                    room = r;
                    break;
                }
            }
            if (room == null) {
                System.out.println("Conference room " + roomName + " does not exist on floor " + floorNumber + " in building " + buildingName + ".");
                return;
            }
            try {
                room.bookRoom(start, end);
                System.out.println("Booked " + roomName + " from " + start + ":" + end);
            } catch (RoomNotAvailableException | InvalidBookingException e) {
                System.out.println(e.getMessage());
            }
        } finally {
            serviceLock.unlock();
        }
    }

    // Cancels a booking
    public void cancelBooking(String buildingName, int floorNumber, String roomName, int start, int end) {
        serviceLock.lock();
        try {
            Building building = buildings.get(buildingName);
            if (building == null) {
                System.out.println("Building " + buildingName + " does not exist.");
                return;
            }
            Floor floor = building.floors.get(floorNumber);
            if (floor == null) {
                System.out.println("Floor " + floorNumber + " does not exist in building " + buildingName + ".");
                return;
            }
            ConferenceRoom room = null;
            for (ConferenceRoom r : floor.rooms) {
                if (r.name.equals(roomName)) {
                    room = r;
                    break;
                }
            }
            if (room == null) {
                System.out.println("Conference room " + roomName + " does not exist on floor " + floorNumber + " in building " + buildingName + ".");
                return;
            }
            room.cancelBooking(start, end);
            System.out.println("Cancelled booking for " + roomName + " from " + start + ":" + end);
        } finally {
            serviceLock.unlock();
        }
    }

    // Lists all bookings across all conference rooms in the format:
    // start:end floor building room
    public void listBookings() {
        serviceLock.lock();
        try {
            for (Building building : buildings.values()) {
                for (Floor floor : building.floors.values()) {
                    for (ConferenceRoom room : floor.rooms) {
                        List<int[]> roomBookings = room.getBookings();
                        for (int[] slot : roomBookings) {
                            System.out.println(slot[0] + ":" + slot[1] + " " + floor.floorNumber + " " + building.name + " " + room.name);
                        }
                    }
                }
            }
        } finally {
            serviceLock.unlock();
        }
    }

    // Suggests up to 3 future available slots for the given duration if no room is available for the given slot.
    // It iterates over all rooms and finds the earliest available slot (after the desired start) where a room is free.
    public void suggestSlots(int desiredStart, int desiredEnd) {
        int duration = desiredEnd - desiredStart;
        List<String> suggestions = new ArrayList<>();
        serviceLock.lock();
        try {
            // For each room, try to find a free slot starting at or after desiredStart
            for (Building building : buildings.values()) {
                for (Floor floor : building.floors.values()) {
                    for (ConferenceRoom room : floor.rooms) {
                        // Search from desiredStart to end of day (24)
                        for (int time = desiredStart; time <= 24 - duration; time++) {
                            if (room.isAvailable(time, time + duration)) {
                                String suggestion = room.name + " " + floor.floorNumber + " " + building.name + " available at " + time + ":" + (time + duration);
                                suggestions.add(suggestion);
                                break; // Suggest only the earliest available slot for this room
                            }
                        }
                    }
                }
            }
        } finally {
            serviceLock.unlock();
        }
        if (suggestions.isEmpty()) {
            System.out.println("No suggestions available.");
        } else {
            // Sort suggestions based on time (if needed) and limit to 3 suggestions
            // For simplicity, we display the first three found.
            System.out.println("Suggestions:");
            int count = 0;
            for (String s : suggestions) {
                System.out.println(s);
                count++;
                if (count == 3) break;
            }
        }
    }
}

// ---------- Driver Class (Command Processor) ----------
public class ConferenceRoomManager {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ConferenceRoomService service = ConferenceRoomService.getInstance();

        // Command processing loop (Enter EXIT to quit)
        while (true) {
            String command = scanner.next();
            if (command.equalsIgnoreCase("EXIT")) {
                break;
            }
            switch (command.toUpperCase()) {
                case "ADD":
                    String type = scanner.next();
                    if (type.equalsIgnoreCase("BUILDING")) {
                        String buildingName = scanner.next();
                        service.addBuilding(buildingName);
                    } else if (type.equalsIgnoreCase("FLOOR")) {
                        String buildingName = scanner.next();
                        int floorNumber = scanner.nextInt();
                        service.addFloor(buildingName, floorNumber);
                    } else if (type.equalsIgnoreCase("CONFROOM")) {
                        String roomName = scanner.next();
                        int floorNumber = scanner.nextInt();
                        String buildingName = scanner.next();
                        service.addConferenceRoom(buildingName, floorNumber, roomName);
                    } else {
                        System.out.println("Invalid ADD command type.");
                    }
                    break;

                case "LIST":
                    String listType = scanner.next();
                    if (listType.equalsIgnoreCase("ROOMS")) {
                        service.listRooms();
                    } else if (listType.equalsIgnoreCase("BOOKING")) {
                        service.listBookings();
                    } else {
                        System.out.println("Invalid LIST command.");
                    }
                    break;

                case "BOOK":
                    // Expected format: BOOK <start>:<end> <buildingName> <floorNumber> <confRoomName>
                    String slot = scanner.next();
                    String[] times = slot.split(":");
                    int start = Integer.parseInt(times[0]);
                    int end = Integer.parseInt(times[1]);
                    String buildingName = scanner.next();
                    int floorNumber = scanner.nextInt();
                    String roomName = scanner.next();
                    service.bookRoom(buildingName, floorNumber, roomName, start, end);
                    break;

                case "CANCEL":
                    // Expected format: CANCEL <start>:<end> <buildingName> <floorNumber> <confRoomName>
                    String cancelSlot = scanner.next();
                    String[] cancelTimes = cancelSlot.split(":");
                    int cStart = Integer.parseInt(cancelTimes[0]);
                    int cEnd = Integer.parseInt(cancelTimes[1]);
                    String bName = scanner.next();
                    int fNumber = scanner.nextInt();
                    String rName = scanner.next();
                    service.cancelBooking(bName, fNumber, rName, cStart, cEnd);
                    break;

                case "SUGGEST":
                    // Expected format: SUGGEST <start>:<end>\n e.g., SUGGEST 3:10\n\n Suggest up to 3 alternatives\n                    \n",
                    String suggestSlot = scanner.next();
                    String[] sTimes = suggestSlot.split(":");
                    int sStart = Integer.parseInt(sTimes[0]);
                    int sEnd = Integer.parseInt(sTimes[1]);
                    service.suggestSlots(sStart, sEnd);
                    break;

                default:
                    System.out.println("Invalid command.");
            }
        }
        scanner.close();
    }
}
