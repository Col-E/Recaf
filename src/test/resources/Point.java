public class Point {
	private int x, y;
	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}
	public void move(int dx, int dy) {
		x += dx;
		y += dy;
	}
}