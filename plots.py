import matplotlib.pyplot as plt
import numpy as np
from scipy.interpolate import interp1d

def sum_of_powers(end, base = 2, start = 1):
  return sum([(base) ** i for i in range(start, end + 1)])

# Example data points
x = np.array([1, 2, 3, 4, 5])
y = np.array([sum_of_powers(1), sum_of_powers(2), sum_of_powers(3), sum_of_powers(4), sum_of_powers(5)])

z = np.array([0,1,2,3,4])
w = np.array([1,3,5,7,9])

# Interpolation function
interp_func = interp1d(x, y, kind='cubic')  # Cubic interpolation for smooth curve
interp_func2 = interp1d(x, z, kind='cubic')  # Cubic interpolation for smooth curve
interp_func3 = interp1d(x, w, kind='cubic')  # Cubic interpolation for smooth curve

# Generate more points for smoother curve
x_interp = np.linspace(min(x), max(x), 100)
y_interp = interp_func(x_interp)
z_interp = interp_func2(x_interp)
w_interp = interp_func3(x_interp)

# Plotting the chart
plt.figure(figsize=(10, 6))
plt.scatter(x, y)
plt.scatter(x, z)
plt.scatter(x, w)
plt.plot(x_interp, y_interp, label='KTables Only', color='red')
plt.plot(x_interp, z_interp, label='Single KStream', color='blue')
plt.plot(x_interp, w_interp, label='Full KStream', color='orange')

# Adding labels and title
plt.xlabel('Num Tables')
plt.ylabel('State Memory')
plt.title('State Memory vs Num Tables')

# Adding a legend
plt.legend()

# Display the chart
plt.grid(True)
plt.show()
