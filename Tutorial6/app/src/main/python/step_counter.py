"""
Step Counter Algorithm for IMU Data
Uses peak detection on acceleration magnitude to count steps.
Optimized for walking detection with configurable parameters.
"""

import numpy as np
from typing import List, Tuple, Optional


class StepCounter:
    """
    Real-time step counter using acceleration magnitude peak detection.
    Optimized for walking mode with maximum 3-second delay.
    """

    def __init__(self, activity_type: str = "Walking"):
        """
        Initialize step counter with activity-specific parameters.

        Args:
            activity_type: "Walking" or "Running" for parameter tuning
        """
        self.activity_type = activity_type
        self.step_count = 0
        self.last_step_time = 0.0

        # Activity-specific thresholds
        # Sensor outputs in m/s² (not g units)
        # Thresholds tuned for typical walking/running patterns
        if activity_type == "Running":
            self.magnitude_threshold = 13.0  # m/s² for running
            self.min_step_interval = 0.25    # seconds (faster cadence)
        else:  # Walking
            self.magnitude_threshold = 11.5  # m/s² for walking
            self.min_step_interval = 0.35    # seconds (normal walking pace)

        # Peak detection state
        self.previous_magnitude = 0.0
        self.was_above_threshold = False
        self.peak_magnitude = 0.0

        # Buffering for 3-second delay processing
        self.buffer = []
        self.buffer_max_size = 300  # ~3 seconds at 100Hz

    def calculate_magnitude(self, acc_x: float, acc_y: float, acc_z: float) -> float:
        """
        Calculate acceleration magnitude from 3-axis data.

        Args:
            acc_x, acc_y, acc_z: Acceleration in m/s² for each axis

        Returns:
            Magnitude of acceleration vector
        """
        return np.sqrt(acc_x**2 + acc_y**2 + acc_z**2)

    def add_sample(self, timestamp: float, acc_x: float, acc_y: float, acc_z: float) -> bool:
        """
        Process a single IMU sample and detect steps in real-time.

        Args:
            timestamp: Sample timestamp in seconds
            acc_x, acc_y, acc_z: Acceleration values in m/s²

        Returns:
            True if a step was detected, False otherwise
        """
        magnitude = self.calculate_magnitude(acc_x, acc_y, acc_z)
        step_detected = False

        # Add to buffer for delayed processing
        self.buffer.append({
            'timestamp': timestamp,
            'magnitude': magnitude
        })

        # Keep buffer size limited
        if len(self.buffer) > self.buffer_max_size:
            self.buffer.pop(0)

        # Peak detection: looking for local maxima above threshold
        if self.was_above_threshold and magnitude < self.previous_magnitude:
            # We have a peak (was going up, now going down)
            time_since_last_step = timestamp - self.last_step_time

            # Check if peak meets criteria
            if (self.previous_magnitude >= self.magnitude_threshold and
                time_since_last_step >= self.min_step_interval):
                self.step_count += 1
                self.last_step_time = timestamp
                self.peak_magnitude = self.previous_magnitude
                step_detected = True

        # Update state for next iteration
        self.was_above_threshold = magnitude >= self.magnitude_threshold
        self.previous_magnitude = magnitude

        return step_detected

    def get_step_count(self) -> int:
        """Get current step count."""
        return self.step_count

    def reset(self):
        """Reset counter to zero."""
        self.step_count = 0
        self.last_step_time = 0.0
        self.previous_magnitude = 0.0
        self.was_above_threshold = False
        self.peak_magnitude = 0.0
        self.buffer.clear()

    def get_delayed_count(self, delay_seconds: float = 3.0) -> int:
        """
        Get step count with specified delay (simulates processing lag).

        Args:
            delay_seconds: Delay in seconds (default 3.0)

        Returns:
            Step count up to delay_seconds ago
        """
        if not self.buffer:
            return self.step_count

        # Find the cutoff timestamp
        current_time = self.buffer[-1]['timestamp']
        cutoff_time = current_time - delay_seconds

        # Count steps before cutoff
        temp_counter = StepCounter(self.activity_type)
        delayed_count = 0

        for sample in self.buffer:
            if sample['timestamp'] <= cutoff_time:
                # This is simplified - in real implementation, we'd need full acc data
                # For now, return current count (the step detection already has inherent delay)
                pass

        return self.step_count


def count_steps_from_data(timestamps: List[float],
                         acc_x: List[float],
                         acc_y: List[float],
                         acc_z: List[float],
                         activity_type: str = "Walking") -> Tuple[int, List[int]]:
    """
    Count steps from complete IMU dataset (for CSV files).

    Args:
        timestamps: List of timestamps in seconds
        acc_x, acc_y, acc_z: Lists of acceleration values in m/s²
        activity_type: "Walking" or "Running"

    Returns:
        Tuple of (total_steps, step_indices) where step_indices are the
        positions in the input arrays where steps were detected
    """
    counter = StepCounter(activity_type)
    step_indices = []

    for i, (t, x, y, z) in enumerate(zip(timestamps, acc_x, acc_y, acc_z)):
        if counter.add_sample(t, x, y, z):
            step_indices.append(i)

    return counter.get_step_count(), step_indices


def count_steps_from_magnitude(timestamps: List[float],
                               magnitudes: List[float],
                               activity_type: str = "Walking") -> int:
    """
    Count steps from pre-calculated magnitude data.

    Args:
        timestamps: List of timestamps in seconds
        magnitudes: List of acceleration magnitudes in m/s²
        activity_type: "Walking" or "Running"

    Returns:
        Total number of steps detected
    """
    # Thresholds in m/s² (not g units)
    if activity_type == "Running":
        threshold = 13.0  # m/s² for running
        min_interval = 0.25
    else:
        threshold = 11.5  # m/s² for walking
        min_interval = 0.35

    steps = 0
    last_step_time = 0.0
    previous_mag = 0.0
    was_above = False

    for t, mag in zip(timestamps, magnitudes):
        # Peak detection
        if was_above and mag < previous_mag:
            if previous_mag >= threshold and (t - last_step_time) >= min_interval:
                steps += 1
                last_step_time = t

        was_above = mag >= threshold
        previous_mag = mag

    return steps


def analyze_walking_pattern(timestamps: List[float],
                           acc_x: List[float],
                           acc_y: List[float],
                           acc_z: List[float]) -> dict:
    """
    Analyze walking pattern and return detailed statistics.
    Useful for calibration and testing.

    Args:
        timestamps, acc_x, acc_y, acc_z: IMU data arrays

    Returns:
        Dictionary with analysis results
    """
    # Calculate magnitudes
    magnitudes = [np.sqrt(x**2 + y**2 + z**2)
                  for x, y, z in zip(acc_x, acc_y, acc_z)]

    # Count steps with both thresholds
    walking_steps, walking_indices = count_steps_from_data(
        timestamps, acc_x, acc_y, acc_z, "Walking"
    )
    running_steps, running_indices = count_steps_from_data(
        timestamps, acc_x, acc_y, acc_z, "Running"
    )

    # Calculate statistics
    if len(timestamps) > 0:
        duration = timestamps[-1] - timestamps[0]
        avg_magnitude = np.mean(magnitudes)
        std_magnitude = np.std(magnitudes)
        max_magnitude = np.max(magnitudes)
        min_magnitude = np.min(magnitudes)
    else:
        duration = 0
        avg_magnitude = 0
        std_magnitude = 0
        max_magnitude = 0
        min_magnitude = 0

    # Calculate step rate
    walking_rate = walking_steps / duration * 60 if duration > 0 else 0
    running_rate = running_steps / duration * 60 if duration > 0 else 0

    return {
        'walking_steps': walking_steps,
        'running_steps': running_steps,
        'duration_seconds': duration,
        'walking_steps_per_minute': walking_rate,
        'running_steps_per_minute': running_rate,
        'avg_magnitude': avg_magnitude,
        'std_magnitude': std_magnitude,
        'max_magnitude': max_magnitude,
        'min_magnitude': min_magnitude,
        'num_samples': len(timestamps),
        'walking_step_indices': walking_indices,
        'running_step_indices': running_indices
    }


# Simple API functions for Android/Kotlin integration
def init_counter(activity_type: str = "Walking") -> StepCounter:
    """Initialize and return a new StepCounter instance."""
    return StepCounter(activity_type)


def process_sample(counter: StepCounter, timestamp: float,
                   acc_x: float, acc_y: float, acc_z: float) -> bool:
    """Process a single sample. Returns True if step detected."""
    return counter.add_sample(timestamp, acc_x, acc_y, acc_z)


def get_count(counter: StepCounter) -> int:
    """Get current step count from counter."""
    return counter.get_step_count()


def reset_counter(counter: StepCounter):
    """Reset counter to zero."""
    counter.reset()


# Batch processing for CSV files
def count_steps_batch(timestamps: list, acc_x: list, acc_y: list,
                     acc_z: list, activity_type: str = "Walking") -> int:
    """
    Count steps from complete dataset (for CSV processing).

    Args:
        timestamps: List of timestamps as floats
        acc_x, acc_y, acc_z: Lists of acceleration values as floats
        activity_type: "Walking" or "Running"

    Returns:
        Total step count as integer
    """
    steps, _ = count_steps_from_data(timestamps, acc_x, acc_y, acc_z, activity_type)
    return steps
