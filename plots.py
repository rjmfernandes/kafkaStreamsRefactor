import matplotlib.pyplot as plt
import numpy as np
from scipy.interpolate import interp1d

def sum_of_powers(end, base = 2, start = 2):
  return sum([(base) ** i for i in range(start, end + 1)])

def ktables(numberOfTables):
  return 2*numberOfTables-2+sum_of_powers(numberOfTables)

def singleStream(numberOfTables):
  return numberOfTables-1;

def fullStream(numberOfTables):
  return 2*numberOfTables-1;

def minimalTable(numberOfTables):
  return 3*numberOfTables-2;

# Example data points
x = np.array([3, 4, 5, 6, 7])
y = np.array([ktables(3), ktables(4), ktables(5),ktables(6),ktables(7)])

z = np.array([singleStream(3), singleStream(4), singleStream(5),singleStream(6),singleStream(7)])
w = np.array([fullStream(3), fullStream(4), fullStream(5),fullStream(6),fullStream(7)])
t = np.array([minimalTable(3), minimalTable(4), minimalTable(5),minimalTable(6),minimalTable(7)])

# Interpolation function
interp_func = interp1d(x, y, kind='cubic')  # Cubic interpolation for smooth curve
interp_func2 = interp1d(x, z, kind='cubic')  # Cubic interpolation for smooth curve
interp_func3 = interp1d(x, w, kind='cubic')  # Cubic interpolation for smooth curve
interp_func4 = interp1d(x, t, kind='cubic')  # Cubic interpolation for smooth curve

# Generate more points for smoother curve
x_interp = np.linspace(min(x), max(x), 100)
y_interp = interp_func(x_interp)
z_interp = interp_func2(x_interp)
w_interp = interp_func3(x_interp)
t_interp = interp_func4(x_interp)

# Plotting the chart
plt.figure(figsize=(10, 6))
plt.scatter(x, y)
plt.scatter(x, z)
plt.scatter(x, w)
plt.scatter(x, t)
plt.plot(x_interp, y_interp, label='0,1 KTables Only', color='red')
plt.plot(x_interp, z_interp, label='2 Single KStream', color='blue')
plt.plot(x_interp, w_interp, label='3 Full KStream', color='orange')
plt.plot(x_interp, t_interp, label='4 Minimal KTables', color='green')

# Adding labels and title
plt.xlabel('Num Tables')
plt.ylabel('State Memory')
plt.title('State Memory vs Num Tables')

# Adding a legend
plt.legend()

# Display the chart
plt.grid(True)
plt.show()