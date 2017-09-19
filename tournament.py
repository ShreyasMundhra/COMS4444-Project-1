import subprocess, os

FNULL = open(os.devnull, "w")
player = "g2c"
num_handles = 1000
repetition = 1
results = []

for i in range(repetition):
    p = open("tmp.log", "w")
    subprocess.run(["java", "escape.sim.Simulator", "-p", player, "-d", str(num_handles)], stdout = p, stderr = FNULL)
    p.close
    with open("tmp.log", "rb") as log:
        log.seek(-2, 2)
        while log.read(1) != b"\n":
            log.seek(-2, 1)
        last = log.readline()
        parsed = [int(s) for s in last.split() if s.isdigit()]
        if (len(parsed) == 0):
            results.append(-1)
        results.extend(parsed)
        log.close()
    #exec cmd
print("Results: " + str(results))
import statistics
print("Median: " + str(statistics.median(results)))
print("Average: " + str(statistics.mean(results)))
print("Standard deviation: " + str(statistics.pstdev(results)))
