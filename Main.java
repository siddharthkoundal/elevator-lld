package Elevator;

public class Main {
    public static void main(String[] args) {
        ElevatorController controller = new ElevatorController();

        // Test 1: Pickup request, elevator moves to floor
        System.out.println("--- Test 1: Basic pickup ---");
        controller.requestElevator(5, RequestType.PICKUP_UP);
        for (int i = 0; i < 6; i++) {
            controller.step();
            System.out.println("Elevator 0 at floor: " + controller.getElevators().get(0).getCurrentFloor());
        }

        // Test 2: Passenger selects destination
        System.out.println("--- Test 2: Destination after pickup ---");
        controller.selectFloor(0, 8); // go to floor 8
        for (int i = 0; i < 4; i++) {
            controller.step();
            System.out.println("Elevator 0 at floor: " + controller.getElevators().get(0).getCurrentFloor());
        }

        // Test 3: Multiple elevators, nearest idle dispatched
        System.out.println("--- Test 3: Nearest idle dispatch ---");
        ElevatorController c2 = new ElevatorController();
        c2.requestElevator(2, RequestType.PICKUP_DOWN);
        c2.step();
        System.out.println("Dispatched elevator at floor: " + c2.getElevators().get(0).getCurrentFloor());

        // Test 4: Invalid floor rejected
        System.out.println("--- Test 4: Boundary validation ---");
        System.out.println("Floor -1: " + controller.requestElevator(-1, RequestType.PICKUP_UP)); // false
        System.out.println("Floor 10: " + controller.requestElevator(10, RequestType.PICKUP_UP)); // false

        // Test 5: SCAN behavior — elevator reverses direction
        System.out.println("--- Test 5: SCAN reversal ---");
        ElevatorController c3 = new ElevatorController();
        c3.requestElevator(3, RequestType.PICKUP_UP);
        c3.requestElevator(1, RequestType.PICKUP_DOWN);
        for (int i = 0; i < 8; i++) {
            c3.step();
            System.out.println("Floor: " + c3.getElevators().get(0).getCurrentFloor()
                + " Dir: " + c3.getElevators().get(0).getDirection());
        }
    }
}